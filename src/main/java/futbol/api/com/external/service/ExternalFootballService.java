package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.config.FootballApiConfig;
import futbol.api.com.external.dto.LeagueInfo;
import futbol.api.com.external.dto.SeasonsResponse;
import futbol.api.com.external.dto.SyncInProgressException;
import futbol.api.com.external.dto.SyncProgress;
import futbol.api.com.external.mapper.FootballDataMapper;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ExternalFootballService {

    private final FootballApiConfig config;
    private final SyncOrchestrator orchestrator;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Autowired
    public ExternalFootballService(FootballApiConfig config, SyncOrchestrator orchestrator) {
        this.config = config;
        this.orchestrator = orchestrator;
    }

    ExternalFootballService(
            FootballApiProvider apiClient,
            FootballDataMapper mapper,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            FootballApiConfig config,
            TransactionTemplate transactionTemplate,
            RequestCounter requestCounter
    ) {
        this.config = config;
        TeamProcessor teamProcessor = new TeamProcessor(
                apiClient,
                mapper,
                teamRepository,
                playerRepository,
                transactionTemplate);
        LeagueProcessor leagueProcessor = new LeagueProcessor(apiClient, requestCounter, teamProcessor, () -> {});
        this.orchestrator = new SyncOrchestrator(leagueProcessor);
    }

    public UUID syncAll(List<Integer> leagueIds, Integer season, Integer maxTeams) {
        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncInProgressException("A sync is already in progress. Wait for it to complete before starting another.");
        }
        UUID syncId = UUID.randomUUID();
        orchestrator.executeSync(syncId, leagueIds, season, maxTeams, () -> syncInProgress.set(false));
        return syncId;
    }

    public UUID syncAll(Integer leagueId) {
        return syncAll(List.of(leagueId), config.season(), null);
    }

    public List<LeagueInfo> getAvailableLeagues() {
        return config.leagueIds().stream()
                .map(id -> new LeagueInfo(id, SyncConstants.LEAGUE_NAMES.getOrDefault(id, "League " + id)))
                .toList();
    }

    public SeasonsResponse getAvailableSeasons() {
        return new SeasonsResponse(config.seasonMin(), config.seasonMax(), config.season());
    }

    public SyncProgress getProgress(UUID syncId) {
        return orchestrator.getProgress(syncId);
    }

    public void evictStaleProgress() {
        orchestrator.evictStaleProgress();
    }
}
