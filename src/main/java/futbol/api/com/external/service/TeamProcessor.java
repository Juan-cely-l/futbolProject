package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.dto.SyncPlayerResult;
import futbol.api.com.external.dto.SyncTeamResult;
import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.mapper.FootballDataMapper;
import futbol.api.com.external.mapper.FootballDataMapper.PlayerData;
import futbol.api.com.external.mapper.FootballDataMapper.TeamInfo;
import futbol.api.com.models.Player;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
class TeamProcessor {

    private final FootballApiProvider apiClient;
    private final FootballDataMapper mapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TransactionTemplate transactionTemplate;

    TeamProcessor(
            FootballApiProvider apiClient,
            FootballDataMapper mapper,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.transactionTemplate = transactionTemplate;
    }

    TeamProcessingResult processTeam(TeamData teamData, Integer leagueId, Integer season) {
        TeamInfo info = mapper.toTeamInfo(teamData);
        boolean wasCreated;
        Team team = teamRepository.findTeamByNameIgnoreCase(info.name()).orElse(null);
        if (team == null) {
            team = teamRepository.save(buildTeam(info));
            wasCreated = true;
        } else {
            wasCreated = false;
        }

        List<String> errors = new ArrayList<>();
        int[] counts = new int[] {0, 0};

        List<PlayerInfo> players;
        try {
            players = apiClient.getPlayersByTeam(teamData.id(), season, leagueId);
        } catch (Exception e) {
            errors.add(teamData.name() + ": " + e.getMessage());
            return buildResult(team, wasCreated, counts, errors);
        }

        Team processedTeam = team;
        try {
            transactionTemplate.executeWithoutResult(status -> {
                int created = 0;
                int updated = 0;

                for (PlayerInfo playerInfo : players) {
                    if (playerInfo.name() == null) {
                        continue;
                    }
                    PlayerData data = mapper.toPlayerData(playerInfo);
                    String name = playerInfo.name().toLowerCase().trim();

                    if (playerRepository.existsPlayerByNameAndAgeAndTeamName(name, data.age(), processedTeam.getName())) {
                        updated++;
                    } else {
                        playerRepository.save(buildPlayer(playerInfo, data, processedTeam));
                        created++;
                    }
                }

                Double ratio = SyncConstants.BUDGET_TO_SQUAD_RATIO.getOrDefault(leagueId, 0.30);
                Long totalSquadValue = playerRepository.sumValueMarketByTeamId(processedTeam.getId());
                if (totalSquadValue != null) {
                    long budget = Math.max((long) (totalSquadValue * ratio), SyncConstants.MINIMUM_BUDGET);
                    processedTeam.setBudget(budget);
                    teamRepository.save(processedTeam);
                }

                counts[0] = created;
                counts[1] = updated;
            });
        } catch (Exception e) {
            errors.add(teamData.name() + ": " + e.getMessage());
        }

        return buildResult(team, wasCreated, counts, errors);
    }

    private TeamProcessingResult buildResult(Team team, boolean wasCreated, int[] counts, List<String> errors) {
        List<Player> squad = playerRepository.findPlayersByTeam_Name(team.getName());
        List<SyncPlayerResult> playerResults = squad.stream()
                .map(p -> new SyncPlayerResult(
                        p.getName(),
                        p.getPosition() != null ? p.getPosition().name() : null,
                        p.getAge(),
                        p.getPhoto(),
                        p.getGoals(),
                        p.getAssists(),
                        p.getMatches(),
                        p.getValueMarket()))
                .toList();

        return new TeamProcessingResult(
                counts[0],
                counts[1],
                new SyncTeamResult(team.getName(), team.getCountry(), wasCreated, !wasCreated, playerResults),
                List.copyOf(errors)
        );
    }

    private Team buildTeam(FootballDataMapper.TeamInfo info) {
        Team team = new Team();
        team.setName(info.name());
        team.setCountry(info.country());
        team.setBudget(SyncConstants.MINIMUM_BUDGET);
        return team;
    }

    private Player buildPlayer(PlayerInfo playerInfo, FootballDataMapper.PlayerData data, Team team) {
        Player player = new Player();
        player.setName(playerInfo.name().toLowerCase().trim());
        player.setAge(data.age());
        player.setPosition(data.position());
        player.setValueMarket(data.valueMarket());
        player.setPhoto(data.photo());
        player.setGoals(playerInfo.goals() != null ? playerInfo.goals() : 0);
        player.setAssists(playerInfo.assists() != null ? playerInfo.assists() : 0);
        player.setMatches(playerInfo.appearences() != null ? playerInfo.appearences() : 0);
        player.setTeam(team);
        return player;
    }
}
