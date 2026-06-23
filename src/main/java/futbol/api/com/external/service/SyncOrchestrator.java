package futbol.api.com.external.service;

import futbol.api.com.external.dto.SyncProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SyncOrchestrator {

    private final LeagueProcessor leagueProcessor;
    private final ConcurrentHashMap<UUID, SyncProgressState> progressMap = new ConcurrentHashMap<>();

    SyncOrchestrator(LeagueProcessor leagueProcessor) {
        this.leagueProcessor = leagueProcessor;
    }

    @Async("footballSyncExecutor")
    public CompletableFuture<Void> executeSync(
            UUID syncId,
            List<Integer> leagueIds,
            Integer season,
            Integer maxTeams,
            Runnable onComplete
    ) {
        SyncProgressState state = new SyncProgressState(leagueIds, season);
        progressMap.put(syncId, state);

        try {
            for (Integer leagueId : leagueIds) {
                if (state.failed()) {
                    break;
                }
                try {
                    LeagueProcessingResult result = leagueProcessor.processLeague(leagueId, season, maxTeams);
                    state.apply(result);
                } catch (Exception e) {
                    log.error("Error processing league {}: {}", leagueId, e.getMessage(), e);
                    state.markPartial("League " + leagueId + " stopped: " + e.getMessage());
                    break;
                }
            }
            state.complete();
        } catch (Exception e) {
            state.markFailed("Sync failed: " + e.getMessage());
            state.complete();
        } finally {
            onComplete.run();
        }

        SyncProgress progress = state.toProgress();
        log.info("Sync {} complete: status={}, leagues={}/{}, players={}",
                syncId, progress.status(), progress.processedLeagues(), progress.totalLeagues(), progress.playersCreated());
        return CompletableFuture.completedFuture(null);
    }

    public SyncProgress getProgress(UUID syncId) {
        SyncProgressState state = progressMap.get(syncId);
        return state != null ? state.toProgress() : null;
    }

    @Scheduled(fixedRate = 300_000)
    public void evictStaleProgress() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        progressMap.entrySet().removeIf(e -> e.getValue().completedBefore(cutoff));
    }
}
