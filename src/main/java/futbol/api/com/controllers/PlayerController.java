package futbol.api.com.controllers;

import futbol.api.com.dtos.player.CreatePlayerRequest;
import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.player.UpdatePlayerRequest;
import futbol.api.com.dtos.team.PlayerEfficiencyResponse;
import futbol.api.com.services.Player.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/futbix/v1/players")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        PlayerResponse response = playerService.createPlayer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable UUID id) {
        PlayerResponse response = playerService.getPlayerById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> updatePlayer(@PathVariable UUID id, @Valid @RequestBody UpdatePlayerRequest request) {
        PlayerResponse response = playerService.updatePlayer(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> getAllPlayers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        Page<PlayerResponse> response = playerService.getAllPlayers(page, size, sortBy, sortDir, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/efficiency/{id}")
    public ResponseEntity<PlayerEfficiencyResponse> getEfficiency(@PathVariable UUID id) {
        PlayerEfficiencyResponse efficiency = playerService.getEfficiency(id);
        return ResponseEntity.ok(efficiency);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable UUID id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
