package futbol.api.com.controllers;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.team.CreateTeamRequest;
import futbol.api.com.dtos.team.TeamResponse;
import futbol.api.com.dtos.team.TeamValueResponse;
import futbol.api.com.dtos.team.UpdateTeamRequest;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Position;
import futbol.api.com.services.Team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamController unit tests")
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController controller;

    private UUID teamId;
    private TeamResponse teamResponse;
    private CreateTeamRequest createRequest;
    private UpdateTeamRequest updateRequest;
    private PlayerResponse playerResponse;

    @BeforeEach
    void setUp() {
        teamId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        teamResponse = TeamResponse.builder()
                .id(teamId)
                .name("FC Barcelona")
                .budget(500_000_000L)
                .city("Barcelona")
                .build();

        createRequest = new CreateTeamRequest();
        createRequest.setName("FC Barcelona");
        createRequest.setBudget(500_000_000L);
        createRequest.setCity("Barcelona");

        updateRequest = new UpdateTeamRequest();
        updateRequest.setName("FC Barcelona Updated");
        updateRequest.setBudget(600_000_000L);
        updateRequest.setCity("Barcelona");

        playerResponse = PlayerResponse.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .name("Lionel Messi")
                .goals(30)
                .position(Position.FORWARD)
                .age(36)
                .assists(15)
                .matches(35)
                .valueMarket(50_000_000)
                .teamName("FC Barcelona")
                .build();
    }

    @Test
    @DisplayName("POST /futbix/v1/teams -> 201 with created team body")
    void createTeam_validRequest_returnsCreated() {
        when(teamService.createTeam(any(CreateTeamRequest.class))).thenReturn(teamResponse);

        ResponseEntity<TeamResponse> response = controller.createTeam(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(teamId);
        assertThat(response.getBody().getName()).isEqualTo("FC Barcelona");
        assertThat(response.getBody().getBudget()).isEqualTo(500_000_000L);
        assertThat(response.getBody().getCity()).isEqualTo("Barcelona");
    }

    @Test
    @DisplayName("GET /futbix/v1/teams -> 200 with team list")
    void getAllTeams_returnsOk() {
        Page<TeamResponse> page = new PageImpl<>(List.of(teamResponse));
        when(teamService.getAllTeams(anyInt(), anyInt(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(page);

        ResponseEntity<Page<TeamResponse>> response = controller.getAllTeams(0, 10, "name", "asc", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getName()).isEqualTo("FC Barcelona");
    }

    @Test
    @DisplayName("GET /futbix/v1/teams when empty -> 200 with empty list")
    void getAllTeams_emptyList_returnsOk() {
        Page<TeamResponse> page = new PageImpl<>(List.of());
        when(teamService.getAllTeams(anyInt(), anyInt(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(page);

        ResponseEntity<Page<TeamResponse>> response = controller.getAllTeams(0, 10, "name", "asc", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/name/{name} -> 200 with matching team")
    void getTeamByName_returnsOk() {
        when(teamService.getTeambyName("FC Barcelona")).thenReturn(teamResponse);

        ResponseEntity<TeamResponse> response = controller.getTeamByName("FC Barcelona");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("FC Barcelona");
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/name/{name} not found -> ResourceNotFoundException")
    void getTeamByName_notFound_throwsException() {
        when(teamService.getTeambyName("NonExistent"))
                .thenThrow(new ResourceNotFoundException("Team not found with name: NonExistent"));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getTeamByName("NonExistent")
        );
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{id} -> 200 with team")
    void getTeamById_returnsOk() {
        when(teamService.getTeamById(teamId)).thenReturn(teamResponse);

        ResponseEntity<TeamResponse> response = controller.getTeamById(teamId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(teamId);
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{id} not found -> ResourceNotFoundException")
    void getTeamById_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(teamService.getTeamById(unknownId))
                .thenThrow(new ResourceNotFoundException("Team not found with id: " + unknownId));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getTeamById(unknownId)
        );
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{name}/squad -> 200 with player list")
    void getTeamSquad_returnsOk() {
        when(teamService.getTeamSquad("FC Barcelona")).thenReturn(List.of(playerResponse));

        ResponseEntity<List<PlayerResponse>> response = controller.getTeamSquad("FC Barcelona");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Lionel Messi");
        assertThat(response.getBody().get(0).getPosition()).isEqualTo(Position.FORWARD);
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{name}/squad not found -> ResourceNotFoundException")
    void getTeamSquad_notFound_throwsException() {
        when(teamService.getTeamSquad("NonExistent"))
                .thenThrow(new ResourceNotFoundException("Team not found with name: NonExistent"));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getTeamSquad("NonExistent")
        );
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{name}/value -> 200 with market value string")
    void getTeamValue_returnsOk() {
        TeamValueResponse valueResponse = TeamValueResponse.builder()
                .teamName("FC Barcelona")
                .totalValue(500_000_000L)
                .build();
        when(teamService.getTeamValue("FC Barcelona")).thenReturn(valueResponse);

        ResponseEntity<TeamValueResponse> response = controller.getTeamValue("FC Barcelona");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTeamName()).isEqualTo("FC Barcelona");
        assertThat(response.getBody().getTotalValue()).isEqualTo(500_000_000L);
    }

    @Test
    @DisplayName("GET /futbix/v1/teams/{name}/value not found -> ResourceNotFoundException")
    void getTeamValue_notFound_throwsException() {
        when(teamService.getTeamValue("NonExistent"))
                .thenThrow(new ResourceNotFoundException("Team not found with name: NonExistent"));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getTeamValue("NonExistent")
        );
    }

    @Test
    @DisplayName("PUT /futbix/v1/teams/{id} -> 200 with updated team")
    void updateTeam_validRequest_returnsOk() {
        TeamResponse updatedResponse = TeamResponse.builder()
                .id(teamId)
                .name("FC Barcelona Updated")
                .budget(600_000_000L)
                .city("Barcelona")
                .build();

        when(teamService.updateTeam(eq(teamId), any(UpdateTeamRequest.class))).thenReturn(updatedResponse);

        ResponseEntity<TeamResponse> response = controller.updateTeam(teamId, updateRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("FC Barcelona Updated");
        assertThat(response.getBody().getBudget()).isEqualTo(600_000_000L);
    }

    @Test
    @DisplayName("PUT /futbix/v1/teams/{id} not found -> ResourceNotFoundException")
    void updateTeam_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(teamService.updateTeam(eq(unknownId), any(UpdateTeamRequest.class)))
                .thenThrow(new ResourceNotFoundException("Team not found with id: " + unknownId));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.updateTeam(unknownId, updateRequest)
        );
    }

    @Test
    @DisplayName("DELETE /futbix/v1/teams/{id} -> 204 No Content")
    void deleteTeam_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteTeam(teamId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(teamService).deleteTeam(teamId);
    }
}
