package futbol.api.com.external.service;

import futbol.api.com.external.dto.Status;
import futbol.api.com.external.dto.SyncProgress;
import futbol.api.com.external.dto.SyncTeamResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncOrchestrator Unit Tests")
class SyncOrchestratorTest {

    @Mock
    private LeagueProcessor leagueProcessor;

    private SyncOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SyncOrchestrator(leagueProcessor);
    }

    @Test
    @DisplayName("executeSync: all leagues complete sets success and completion timestamp")
    void executeSync_allLeaguesComplete_setsSuccessAndCompletionTimestamp() {
        UUID syncId = UUID.randomUUID();
        when(leagueProcessor.processLeague(39, 2025, null))
                .thenReturn(LeagueProcessingResult.success(1, 1, 2, 0, List.of()));

        orchestrator.executeSync(syncId, List.of(39), 2025, null, () -> {});

        SyncProgress progress = orchestrator.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.processedLeagues()).isEqualTo(1);
        assertThat(progress.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("executeSync: league processor failure records partial status and error")
    void executeSync_leagueProcessorFails_recordsPartialStatusAndError() {
        UUID syncId = UUID.randomUUID();
        when(leagueProcessor.processLeague(39, 2025, null))
                .thenThrow(new RuntimeException("league failed"));

        orchestrator.executeSync(syncId, List.of(39), 2025, null, () -> {});

        SyncProgress progress = orchestrator.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.PARTIAL);
        assertThat(progress.errors()).contains("League 39 stopped: league failed");
        assertThat(progress.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("executeSync: later league failure preserves earlier progress and marks partial")
    void executeSync_laterLeagueFails_preservesEarlierProgressAndMarksPartial() {
        UUID syncId = UUID.randomUUID();
        SyncTeamResult arsenal = new SyncTeamResult("arsenal", "England", true, false, List.of());
        when(leagueProcessor.processLeague(39, 2025, null))
                .thenReturn(LeagueProcessingResult.success(2, 2, 4, 1, List.of(arsenal)));
        when(leagueProcessor.processLeague(140, 2025, null))
                .thenThrow(new RuntimeException("league failed"));

        orchestrator.executeSync(syncId, List.of(39, 140), 2025, null, () -> {});

        SyncProgress progress = orchestrator.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.PARTIAL);
        assertThat(progress.totalLeagues()).isEqualTo(2);
        assertThat(progress.processedLeagues()).isEqualTo(1);
        assertThat(progress.totalTeams()).isEqualTo(2);
        assertThat(progress.processedTeams()).isEqualTo(2);
        assertThat(progress.playersCreated()).isEqualTo(4);
        assertThat(progress.playersUpdated()).isEqualTo(1);
        assertThat(progress.errors()).contains("League 140 stopped: league failed");
        assertThat(progress.teams()).containsExactly(arsenal);
        assertThat(progress.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("executeSync: estimated requests exceeding remaining skips league and records partial progress")
    void executeSync_estimatedRequestsExceedRemaining_skipsLeagueAndRecordsPartialProgress() {
        UUID syncId = UUID.randomUUID();
        when(leagueProcessor.processLeague(39, 2025, null))
                .thenReturn(LeagueProcessingResult.skipped(11, "League 39 skipped: only 3 requests remaining, ~11 needed"));

        orchestrator.executeSync(syncId, List.of(39), 2025, null, () -> {});

        SyncProgress progress = orchestrator.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.PARTIAL);
        assertThat(progress.processedLeagues()).isZero();
        assertThat(progress.errors()).contains("League 39 skipped: only 3 requests remaining, ~11 needed");
    }
}
