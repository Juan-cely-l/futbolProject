package futbol.api.com.external;

import futbol.api.com.exceptions.ExternalApiException;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.dto.player.ApiPlayerResponse;
import futbol.api.com.external.dto.player.PlayerEntry;
import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.player.PlayerStat;
import futbol.api.com.external.dto.team.ApiTeamResponse;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.dto.team.TeamEntry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ApiFootballClient implements FootballApiProvider{
    private final RestClient restClient;
    private final RequestCounter requestCounter;

    public ApiFootballClient(RestClient footballRestClient,RequestCounter counter){
        this.restClient=footballRestClient;
        this.requestCounter=counter;
    }
    @Override
    public List<TeamData> getTeamsByLeague(Integer leagueId, Integer season) {
        requestCounter.increment();
        ApiTeamResponse response=restClient.get()
                .uri("/teams?league={leagueId}&season={season}", leagueId,season)
                .retrieve()
                .onStatus(status->!status.is2xxSuccessful(),(req,res)->{
                            throw new ExternalApiException(
                                    res.getStatusCode().value(),
                                    "API-Football-error"+res.getStatusText()
                            );
                        }
                )
                .body(ApiTeamResponse.class);
        Objects.requireNonNull(response);
        return response.response().stream()
                .map(TeamEntry::team)
                .toList();
    }

    @Override
    public List<PlayerInfo> getPlayersByTeam(Integer teamId, Integer season, Integer leagueId) {
        List<PlayerInfo> allPlayers = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            requestCounter.increment();
            ApiPlayerResponse response = restClient.get()
                    .uri("/players?team={teamId}&season={season}&league={leagueId}&page={page}", teamId, season, leagueId, page)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new ExternalApiException(
                                res.getStatusCode().value(),
                                "API-Football-error" + res.getStatusText()
                        );
                    })
                    .body(ApiPlayerResponse.class);

            Objects.requireNonNull(response);

            List<PlayerInfo> pagePlayers = response.response().stream()
                    .map(entry -> {
                        PlayerInfo p = entry.player();
                        PlayerStat stats = entry.statistics() != null && !entry.statistics().isEmpty()
                                ? entry.statistics().getFirst()
                                : null;
                        Integer goals = stats != null && stats.goals() != null ? stats.goals().total() : null;
                        Integer assists = stats != null && stats.goals() != null ? stats.goals().assists() : null;
                        Integer appearences = stats != null && stats.games() != null ? stats.games().appearences() : null;
                        String position = stats != null && stats.games() != null ? stats.games().position() : null;
                        return new PlayerInfo(
                                p.id(), p.name(), p.age(), position, p.photo(),
                                goals != null ? goals : 0,
                                assists != null ? assists : 0,
                                appearences != null ? appearences : 0
                        );
                    })
                    .toList();

            allPlayers.addAll(pagePlayers);

            if (response.paging() != null && response.paging().total() != null) {
                totalPages = response.paging().total();
            }
            page++;
        }

        return allPlayers;
    }
}
