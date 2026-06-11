package futbol.api.com.controllers.external;

import futbol.api.com.external.dto.LeagueInfo;
import futbol.api.com.external.dto.SeasonsResponse;
import futbol.api.com.external.dto.SyncProgress;
import futbol.api.com.external.dto.SyncRequest;
import futbol.api.com.external.service.ExternalFootballService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/futbix/v1/sync")
public class SyncController {

    private final ExternalFootballService syncService;

    public SyncController(ExternalFootballService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> startSync(@RequestBody SyncRequest request) {

        List<Integer> leagueIds = request.leagueIds() != null ? request.leagueIds() : List.of(140);
        Integer season = request.season();

        UUID syncId = syncService.syncAll(leagueIds, season, request.maxTeams());

        return ResponseEntity.accepted().body(Map.of(
                "syncId", syncId.toString(),
                "status", "PROCESSING",
                "message", "Sync started for leagues " + leagueIds + " season " + season
        ));
    }

    @GetMapping("/leagues")
    public List<LeagueInfo> getAvailableLeagues() {
        return syncService.getAvailableLeagues();
    }

    @GetMapping("/seasons")
    public SeasonsResponse getAvailableSeasons() {
        return syncService.getAvailableSeasons();
    }

    @GetMapping("/{syncId}")
    public ResponseEntity<?> getProgress(@PathVariable UUID syncId) {
        SyncProgress progress = syncService.getProgress(syncId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
}
