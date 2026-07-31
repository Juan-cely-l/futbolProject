package futbol.api.com.controllers.external;

import futbol.api.com.external.dto.LeagueInfo;
import futbol.api.com.external.dto.SeasonsResponse;
import futbol.api.com.external.dto.Status;
import futbol.api.com.external.dto.SyncAdmissionRejectedException;
import futbol.api.com.external.dto.SyncProgress;
import futbol.api.com.external.dto.SyncRequest;
import futbol.api.com.external.dto.SyncTeamResult;
import futbol.api.com.external.service.ExternalFootballService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncController Unit Tests")
class SyncControllerTest {

    @Mock
    private ExternalFootballService syncService;

    @InjectMocks
    private SyncController controller;

    // -----------------------------------------------------------------------
    // POST /futbix/v1/sync
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /futbix/v1/sync with leagueIds -> 202 Accepted")
    void startSync_withLeagueIds_returnsAccepted() {
        UUID syncId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        SyncRequest request = new SyncRequest(List.of(140, 39), 2024, null);
        when(syncService.syncAll(request.leagueIds(), request.season(), request.maxTeams())).thenReturn(syncId);

        ResponseEntity<Map<String, Object>> response = controller.startSync(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("syncId", syncId.toString());
        assertThat(response.getBody()).containsEntry("status", "PROCESSING");
    }

    @Test
    @DisplayName("POST /futbix/v1/sync with null leagueIds -> defaults to [140]")
    void startSync_nullLeagueIds_defaultsTo140() {
        UUID syncId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        SyncRequest request = new SyncRequest(null, 2024, null);
        when(syncService.syncAll(List.of(140), 2024, null)).thenReturn(syncId);

        ResponseEntity<Map<String, Object>> response = controller.startSync(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("POST /futbix/v1/sync when executor rejects -> propagates SyncAdmissionRejectedException (no 202/syncId)")
    void startSync_whenExecutorRejects_propagatesException() {
        SyncRequest request = new SyncRequest(List.of(140), 2024, null);
        when(syncService.syncAll(request.leagueIds(), request.season(), request.maxTeams()))
                .thenThrow(new SyncAdmissionRejectedException(
                        "Sync could not be started: the sync scheduler is temporarily unavailable. Retry shortly."));

        assertThatThrownBy(() -> controller.startSync(request))
                .isInstanceOf(SyncAdmissionRejectedException.class);
    }

    // -----------------------------------------------------------------------
    // GET /futbix/v1/sync/leagues
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /futbix/v1/sync/leagues -> 200 with available leagues")
    void getAvailableLeagues_returnsLeagues() {
        List<LeagueInfo> leagues = List.of(
                new LeagueInfo(39, "Premier League"),
                new LeagueInfo(140, "La Liga")
        );
        when(syncService.getAvailableLeagues()).thenReturn(leagues);

        List<LeagueInfo> result = controller.getAvailableLeagues();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(39);
    }

    // -----------------------------------------------------------------------
    // GET /futbix/v1/sync/seasons
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /futbix/v1/sync/seasons -> 200 with season range")
    void getAvailableSeasons_returnsSeasons() {
        when(syncService.getAvailableSeasons()).thenReturn(new SeasonsResponse(2010, 2024, 2024));

        SeasonsResponse result = controller.getAvailableSeasons();

        assertThat(result.minSeason()).isEqualTo(2010);
        assertThat(result.maxSeason()).isEqualTo(2024);
    }

    // -----------------------------------------------------------------------
    // GET /futbix/v1/sync/{syncId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /futbix/v1/sync/{syncId} with valid ID -> 200 and SyncProgress")
    void getProgress_validId_returnsProgress() {
        UUID syncId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        SyncProgress progress = new SyncProgress(
                Status.SUCCESS, List.of(140), 1, 1, 10, 10,
                25, 5, 2024, List.of(),
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now(),
                List.of()
        );
        when(syncService.getProgress(syncId)).thenReturn(progress);

        ResponseEntity<?> response = controller.getProgress(syncId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(progress);
    }

    @Test
    @DisplayName("GET /futbix/v1/sync/{syncId} with invalid ID -> 404 Not Found")
    void getProgress_invalidId_returnsNotFound() {
        UUID syncId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        when(syncService.getProgress(syncId)).thenReturn(null);

        ResponseEntity<?> response = controller.getProgress(syncId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("GET /futbix/v1/sync/{syncId} with random UUID -> 404")
    void getProgress_randomId_returnsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(syncService.getProgress(randomId)).thenReturn(null);

        ResponseEntity<?> response = controller.getProgress(randomId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
