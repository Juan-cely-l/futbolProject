package futbol.api.com.controllers;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.team.CreateTeamRequest;
import futbol.api.com.dtos.team.TeamResponse;
import futbol.api.com.dtos.team.TeamValueResponse;
import futbol.api.com.dtos.team.UpdateTeamRequest;
import futbol.api.com.services.Team.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/futbix/v1/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TeamResponse>> getAllTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ) {
        Page<TeamResponse> response = teamService.getAllTeams(page, size, sortBy, sortDir, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<TeamResponse> getTeamByName(@PathVariable String name) {
        TeamResponse response = teamService.getTeambyName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable UUID id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}/squad")
    public ResponseEntity<List<PlayerResponse>> getTeamSquad(@PathVariable String name) {
        List<PlayerResponse> response = teamService.getTeamSquad(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}/value")
    public ResponseEntity<TeamValueResponse> getTeamValue(@PathVariable String name) {
        TeamValueResponse value = teamService.getTeamValue(name);
        return ResponseEntity.ok(value);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse response = teamService.updateTeam(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}
