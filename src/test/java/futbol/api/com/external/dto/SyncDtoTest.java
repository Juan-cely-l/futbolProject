package futbol.api.com.external.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sync DTO and Enum Tests")
class SyncDtoTest {

    @Test
    @DisplayName("SyncRequest: constructor and accessors")
    void syncRequest() {
        var req = new SyncRequest(List.of(1, 2, 3), 2025, null);
        assertThat(req.leagueIds()).containsExactly(1, 2, 3);
        assertThat(req.season()).isEqualTo(2025);
        assertThat(req.maxTeams()).isNull();
    }

    @Test
    @DisplayName("SyncProgress: constructor and accessors")
    void syncProgress() {
        var started = LocalDateTime.of(2025, 1, 1, 10, 0);
        var completed = LocalDateTime.of(2025, 1, 1, 10, 30);
        var team = new SyncTeamResult("FC Barcelona", "Spain", true, false, List.of());
        var progress = new SyncProgress(
                Status.SUCCESS, List.of(1), 2, 2, 3, 3,
                10, 5, 2025, List.of(), started, completed, List.of(team));

        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.leagueIds()).containsExactly(1);
        assertThat(progress.totalLeagues()).isEqualTo(2);
        assertThat(progress.processedLeagues()).isEqualTo(2);
        assertThat(progress.season()).isEqualTo(2025);
        assertThat(progress.startedAt()).isEqualTo(started);
        assertThat(progress.completedAt()).isEqualTo(completed);
        assertThat(progress.teams()).hasSize(1);
    }

    @Test
    @DisplayName("Status: enum values")
    void statusEnum() {
        assertThat(Status.PROCESSING).isIn(Status.values());
        assertThat(Status.SUCCESS).isIn(Status.values());
        assertThat(Status.PARTIAL).isIn(Status.values());
        assertThat(Status.FAILED).isIn(Status.values());
    }

    @Test
    @DisplayName("LeagueInfo: constructor and accessors")
    void leagueInfo() {
        var info = new LeagueInfo(1, "La Liga");
        assertThat(info.id()).isEqualTo(1);
        assertThat(info.name()).isEqualTo("La Liga");
    }

    @Test
    @DisplayName("SeasonsResponse: constructor and accessors")
    void seasonsResponse() {
        var resp = new SeasonsResponse(2020, 2025, 2025);
        assertThat(resp.minSeason()).isEqualTo(2020);
        assertThat(resp.maxSeason()).isEqualTo(2025);
        assertThat(resp.currentSeason()).isEqualTo(2025);
    }

    @Test
    @DisplayName("SyncInProgressException: constructor")
    void syncInProgressException() {
        var ex = new SyncInProgressException("Sync in progress");
        assertThat(ex.getMessage()).isEqualTo("Sync in progress");
    }
}
