package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.dto.SyncTeamResult;
import futbol.api.com.external.dto.team.TeamData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
class LeagueProcessor {

    private final FootballApiProvider apiClient;
    private final RequestCounter requestCounter;
    private final TeamProcessor teamProcessor;
    private final SyncDelay delay;

    @Autowired
    LeagueProcessor(FootballApiProvider apiClient, RequestCounter requestCounter, TeamProcessor teamProcessor) {
        this(apiClient, requestCounter, teamProcessor, SyncDelay.defaultDelay());
    }

    LeagueProcessor(
            FootballApiProvider apiClient,
            RequestCounter requestCounter,
            TeamProcessor teamProcessor,
            SyncDelay delay
    ) {
        this.apiClient = apiClient;
        this.requestCounter = requestCounter;
        this.teamProcessor = teamProcessor;
        this.delay = delay;
    }

    LeagueProcessingResult processLeague(Integer leagueId, Integer season, Integer maxTeams) {
        LeagueData leagueData = estimateRequestsForLeague(leagueId, season, maxTeams);
        int remaining = requestCounter.remaining();
        if (remaining < leagueData.estimatedRequests()) {
            String msg = String.format(
                    "League %d (%s) skipped: only %d requests remaining, ~%d needed",
                    leagueId,
                    SyncConstants.LEAGUE_NAMES.getOrDefault(leagueId, "?"),
                    remaining,
                    leagueData.estimatedRequests());
            log.warn(msg);
            return LeagueProcessingResult.skipped(leagueData.estimatedRequests(), msg);
        }

        List<TeamData> teams = leagueData.teams();
        if (teams.isEmpty()) {
            teams = limited(apiClient.getTeamsByLeague(leagueId, season), maxTeams);
        }

        log.info("Processing league {} ({}): {} teams",
                leagueId,
                SyncConstants.LEAGUE_NAMES.getOrDefault(leagueId, "?"),
                teams.size());

        int playersCreated = 0;
        int playersUpdated = 0;
        int processedTeams = 0;
        List<String> errors = new ArrayList<>();
        List<SyncTeamResult> teamResults = new ArrayList<>();

        for (TeamData teamData : teams) {
            try {
                TeamProcessingResult result = teamProcessor.processTeam(teamData, leagueId, season);
                processedTeams++;
                playersCreated += result.playersCreated();
                playersUpdated += result.playersUpdated();
                errors.addAll(result.errors());
                teamResults.add(result.teamResult());
            } catch (Exception e) {
                log.error("Error processing team {}: {}", teamData.name(), e.getMessage(), e);
                processedTeams++;
                errors.add(teamData.name() + ": " + e.getMessage());
            }
            delay.sleep();
        }

        return new LeagueProcessingResult(
                leagueData.estimatedRequests(),
                teams.size(),
                processedTeams,
                playersCreated,
                playersUpdated,
                true,
                false,
                List.copyOf(errors),
                List.copyOf(teamResults)
        );
    }

    private LeagueData estimateRequestsForLeague(Integer leagueId, Integer season, Integer maxTeams) {
        try {
            List<TeamData> teams = limited(apiClient.getTeamsByLeague(leagueId, season), maxTeams);
            int estimated = (int) (SyncConstants.ESTIMATED_REQUESTS_PER_LEAGUE_FIXED
                    + teams.size() * SyncConstants.ESTIMATED_REQUESTS_PER_TEAM);
            return new LeagueData(estimated, teams);
        } catch (Exception e) {
            return new LeagueData(20, List.of());
        }
    }

    private List<TeamData> limited(List<TeamData> teams, Integer maxTeams) {
        if (maxTeams != null && maxTeams < teams.size()) {
            return teams.subList(0, maxTeams);
        }
        return teams;
    }

    private record LeagueData(int estimatedRequests, List<TeamData> teams) {
    }
}
