package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.config.FootballApiConfig;
import futbol.api.com.external.dto.LeagueInfo;
import futbol.api.com.external.dto.SeasonsResponse;
import futbol.api.com.external.dto.Status;
import futbol.api.com.external.dto.SyncInProgressException;
import futbol.api.com.external.dto.SyncPlayerResult;
import futbol.api.com.external.dto.SyncProgress;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static futbol.api.com.external.dto.Status.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalFootballService {

    private static final long DELAY_BETWEEN_TEAMS_MS = 6_000L;
    private static final long ESTIMATED_REQUESTS_PER_TEAM = 5L;
    private static final long ESTIMATED_REQUESTS_PER_LEAGUE_FIXED = 1L;
    private static final long MINIMUM_BUDGET = 5_000_000L;

    // Budget = squad value * ratio. Represents annual budget as fraction of squad market value.
    private static final Map<Integer, Double> BUDGET_TO_SQUAD_RATIO = Map.of(
            39, 0.50,   // Premier League
            140, 0.40,  // La Liga
            135, 0.35,  // Serie A
            78,  0.35,  // Bundesliga
            61,  0.30   // Ligue 1
    );

    private static final Map<Integer, String> LEAGUE_NAMES = Map.of(
            39, "Premier League",
            140, "La Liga",
            135, "Serie A",
            78, "Bundesliga",
            61, "Ligue 1"
    );

    private final FootballApiProvider apiClient;
    private final FootballDataMapper mapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final FootballApiConfig config;
    private final TransactionTemplate transactionTemplate;
    private final RequestCounter requestCounter;

    private final ConcurrentHashMap<UUID, SyncStats> progressMap = new ConcurrentHashMap<>();
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Lazy
    @Autowired
    private ExternalFootballService self;

    // ─── public API ─────────────────────────────────────

    public UUID syncAll(List<Integer> leagueIds, Integer season, Integer maxTeams) {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncInProgressException("A sync is already in progress. Wait for it to complete before starting another.");
        }
        UUID syncId = UUID.randomUUID();
        self.executeSync(syncId, leagueIds, season, maxTeams);
        return syncId;
    }

    public UUID syncAll(Integer leagueId) {
        return syncAll(List.of(leagueId), config.season(), null);
    }

    public List<LeagueInfo> getAvailableLeagues() {
        return config.leagueIds().stream()
                .map(id -> new LeagueInfo(id, LEAGUE_NAMES.getOrDefault(id, "League " + id)))
                .toList();
    }

    public SeasonsResponse getAvailableSeasons() {
        return new SeasonsResponse(config.seasonMin(), config.seasonMax(), config.season());
    }

    // ─── async sync ─────────────────────────────────────

    @Async("footballSyncExecutor")
    public CompletableFuture<Void> executeSync(UUID syncId, List<Integer> leagueIds, Integer season, Integer maxTeams) {
        SyncStats stats = new SyncStats(leagueIds, season);
        progressMap.put(syncId, stats);

        try {
            for (int i = 0; i < leagueIds.size(); i++) {
                if (stats.status == FAILED) break;

                stats.currentLeagueIndex = i;
                Integer leagueId = leagueIds.get(i);
                stats.totalTeams = 0;

                // Rate limit pre-check (also caches team list to avoid double fetch)
                LeagueData leagueData = estimateRequestsForLeague(leagueId, season, maxTeams);
                if (requestCounter.remaining() < leagueData.estimated()) {
                    String msg = String.format(
                            "League %d (%s) skipped: only %d requests remaining, ~%d needed",
                            leagueId, LEAGUE_NAMES.getOrDefault(leagueId, "?"),
                            requestCounter.remaining(), leagueData.estimated());
                    log.warn(msg);
                    stats.errors.add(msg);
                    continue;
                }

                try {
                    processLeague(leagueId, season, stats, leagueData.teams(), maxTeams);
                } catch (Exception e) {
                    log.error("Error processing league {}: {}", leagueId, e.getMessage(), e);
                    stats.status = PARTIAL;
                    stats.errors.add("League " + leagueId + " stopped: " + e.getMessage());
                    break;
                }
            }

            if (stats.status == PROCESSING) {
                stats.status = stats.errors.isEmpty() ? SUCCESS : PARTIAL;
            }
        } catch (Exception e) {
            stats.status = FAILED;
            stats.errors.add("Sync failed: " + e.getMessage());
        }

        stats.completedAt = LocalDateTime.now();
        log.info("Sync {} complete: status={}, leagues={}/{}, players={}",
                syncId, stats.status, stats.processedLeagues.get(), stats.leagueIds.size(), stats.playersCreated.get());
        syncInProgress.set(false);
        return CompletableFuture.completedFuture(null);
    }

    private record LeagueData(int estimated, List<TeamData> teams) {}

    private LeagueData estimateRequestsForLeague(Integer leagueId, Integer season, Integer maxTeams) {
        try {
            List<TeamData> teams = apiClient.getTeamsByLeague(leagueId, season);
            if (maxTeams != null && maxTeams < teams.size()) {
                teams = teams.subList(0, maxTeams);
            }
            int estimated = (int) (ESTIMATED_REQUESTS_PER_LEAGUE_FIXED + teams.size() * ESTIMATED_REQUESTS_PER_TEAM);
            return new LeagueData(estimated, teams);
        } catch (Exception e) {
            return new LeagueData(20, List.of()); // empty list signals processLeague to fetch fresh
        }
    }

    private void processLeague(Integer leagueId, Integer season, SyncStats stats, List<TeamData> teams, Integer maxTeams) {
        if (teams.isEmpty()) {
            teams = apiClient.getTeamsByLeague(leagueId, season);
        }
        if (maxTeams != null && maxTeams < teams.size()) {
            teams = teams.subList(0, maxTeams);
            log.info("Limited to {} teams (maxTeams={})", teams.size(), maxTeams);
        }
        stats.totalTeams = teams.size();
        log.info("Processing league {} ({}): {} teams", leagueId,
                LEAGUE_NAMES.getOrDefault(leagueId, "?"), teams.size());

        for (TeamData teamData : teams) {
            try {
                processTeam(teamData, leagueId, season, stats);
                stats.processedTeams.incrementAndGet();
            } catch (Exception e) {
                log.error("Error processing team {}: {}", teamData.name(), e.getMessage(), e);
                stats.processedTeams.incrementAndGet();
                stats.errors.add(teamData.name() + ": " + e.getMessage());
            }
            try {
                Thread.sleep(DELAY_BETWEEN_TEAMS_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        stats.processedLeagues.incrementAndGet();
    }

    // ─── per-team processing ────────────────────────────

    private void processTeam(TeamData teamData, Integer leagueId, Integer season, SyncStats stats) {
        log.debug("Processing team: {} (league {})", teamData.name(), leagueId);
        TeamInfo info = mapper.toTeamInfo(teamData);

        // Phase 1: Find or create team (auto-committed — survives player failures)
        boolean wasCreated;
        Team team = teamRepository.findTeamByNameIgnoreCase(info.name()).orElse(null);
        if (team == null) {
            team = teamRepository.save(buildTeam(info, leagueId));
            wasCreated = true;
        } else {
            wasCreated = false;
        }

        // Phase 2: Player processing in its own transaction
        final Team processedTeam = team;
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<PlayerInfo> players = apiClient.getPlayersByTeam(teamData.id(), season, leagueId);
                int created = 0, updated = 0;

                for (PlayerInfo playerInfo : players) {
                    if (playerInfo.name() == null) continue;
                    PlayerData data = mapper.toPlayerData(playerInfo);
                    String name = playerInfo.name().toLowerCase().trim();

                    if (playerRepository.existsPlayerByNameAndAgeAndTeamName(name, data.age(), processedTeam.getName())) {
                        updated++;
                    } else {
                        playerRepository.save(buildPlayer(playerInfo, data, processedTeam));
                        created++;
                    }
                }

                // Recalculate budget from actual squad value
                Double ratio = BUDGET_TO_SQUAD_RATIO.getOrDefault(leagueId, 0.30);
                Long totalSquadValue = playerRepository.sumValueMarketByTeamId(processedTeam.getId());
                if (totalSquadValue != null) {
                    long budget = Math.max((long) (totalSquadValue * ratio), MINIMUM_BUDGET);
                    processedTeam.setBudget(budget);
                    teamRepository.save(processedTeam);
                }

                stats.playersCreated.addAndGet(created);
                stats.playersUpdated.addAndGet(updated);
            });
        } catch (Exception e) {
            log.warn("Player processing failed for team {}: {}", teamData.name(), e.getMessage());
            stats.errors.add(teamData.name() + ": " + e.getMessage());
        }

        // Phase 3: Build sync result
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

        stats.teamResults.add(new SyncTeamResult(
                team.getName(), team.getCountry(), wasCreated, !wasCreated, playerResults));
    }

    // ─── progress ───────────────────────────────────────

    public SyncProgress getProgress(UUID syncId) {
        SyncStats stats = progressMap.get(syncId);
        if (stats == null) return null;
        List<SyncTeamResult> teams = stats.status == PROCESSING
                ? null : List.copyOf(stats.teamResults);
        return new SyncProgress(
                stats.status,
                stats.leagueIds,
                stats.leagueIds.size(),
                stats.processedLeagues.get(),
                stats.totalTeams,
                stats.processedTeams.get(),
                stats.playersCreated.get(),
                stats.playersUpdated.get(),
                stats.season,
                List.copyOf(stats.errors),
                stats.startedAt,
                stats.completedAt,
                teams
        );
    }

    @Scheduled(fixedRate = 300_000)
    public void evictStaleProgress() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        progressMap.entrySet().removeIf(e ->
                e.getValue().completedAt != null &&
                        e.getValue().completedAt.isBefore(cutoff));
    }

    // ─── helpers ────────────────────────────────────────

    private Team buildTeam(FootballDataMapper.TeamInfo info, Integer leagueId) {
        Team team = new Team();
        team.setName(info.name());
        team.setCountry(info.country());
        team.setBudget(MINIMUM_BUDGET);
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

    // ─── inner types ────────────────────────────────────

    private static class SyncStats {
        Status status = PROCESSING;
        final List<Integer> leagueIds;
        final Integer season;
        int currentLeagueIndex = 0;
        int totalTeams;
        final AtomicInteger processedTeams = new AtomicInteger(0);
        final AtomicInteger playersCreated = new AtomicInteger(0);
        final AtomicInteger playersUpdated = new AtomicInteger(0);
        final AtomicInteger processedLeagues = new AtomicInteger(0);
        final List<String> errors = new ArrayList<>();
        final List<SyncTeamResult> teamResults = new ArrayList<>();
        final LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime completedAt;

        SyncStats(List<Integer> leagueIds, Integer season) {
            this.leagueIds = leagueIds;
            this.season = season;
        }
    }
}
