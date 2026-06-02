package futbol.api.com.controllers;

import futbol.api.com.dtos.player.CreatePlayerRequest;
import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.player.UpdatePlayerRequest;
import futbol.api.com.dtos.team.PlayerEfficiencyResponse;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Position;
import futbol.api.com.services.Player.PlayerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerController unit tests")
class PlayerControllerTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerController controller;

    private UUID playerId;
    private PlayerResponse playerResponse;
    private CreatePlayerRequest createRequest;
    private UpdatePlayerRequest updateRequest;

    @BeforeEach
    void setUp() {
        playerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        playerResponse = PlayerResponse.builder()
                .id(playerId)
                .name("Lionel Messi")
                .goals(30)
                .position(Position.FORWARD)
                .age(36)
                .assists(15)
                .matches(35)
                .valueMarket(50_000_000)
                .teamName("FC Barcelona")
                .build();

        createRequest = new CreatePlayerRequest();
        createRequest.setName("Lionel Messi");
        createRequest.setGoals(30);
        createRequest.setPosition(Position.FORWARD);
        createRequest.setAge(36);
        createRequest.setAssists(15);
        createRequest.setMatches(35);
        createRequest.setValueMarket(50_000_000);
        createRequest.setTeamName("FC Barcelona");

        updateRequest = new UpdatePlayerRequest();
        updateRequest.setName("Lionel Messi Updated");
        updateRequest.setGoals(35);
        updateRequest.setPosition(Position.FORWARD);
        updateRequest.setAge(37);
        updateRequest.setAssists(20);
        updateRequest.setMatches(40);
        updateRequest.setValueMarket(55_000_000);
        updateRequest.setTeamName("FC Barcelona");
    }

    @Test
    @DisplayName("POST /futbix/v1/players -> 201 with created player body")
    void createPlayer_validRequest_returnsCreated() {
        when(playerService.createPlayer(any(CreatePlayerRequest.class))).thenReturn(playerResponse);

        ResponseEntity<PlayerResponse> response = controller.createPlayer(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(playerId);
        assertThat(response.getBody().getName()).isEqualTo("Lionel Messi");
        assertThat(response.getBody().getPosition()).isEqualTo(Position.FORWARD);
        assertThat(response.getBody().getAge()).isEqualTo(36);
    }

    @Test
    @DisplayName("GET /futbix/v1/players -> 200 with player list")
    void getAllPlayers_returnsOk() {
        Page<PlayerResponse> playerPage = new PageImpl<>(List.of(playerResponse));
        when(playerService.getAllPlayers(anyInt(), anyInt(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(playerPage);

        ResponseEntity<Page<PlayerResponse>> response = controller.getAllPlayers(0, 10, "name", "asc", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getName()).isEqualTo("Lionel Messi");
    }

    @Test
    @DisplayName("GET /futbix/v1/players when empty -> 200 with empty list")
    void getAllPlayers_emptyList_returnsOk() {
        Page<PlayerResponse> emptyPage = new PageImpl<>(List.of());
        when(playerService.getAllPlayers(anyInt(), anyInt(), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(emptyPage);

        ResponseEntity<Page<PlayerResponse>> response = controller.getAllPlayers(0, 10, "name", "asc", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /futbix/v1/players/{id} -> 200 with player")
    void getPlayerById_returnsOk() {
        when(playerService.getPlayerById(playerId)).thenReturn(playerResponse);

        ResponseEntity<PlayerResponse> response = controller.getPlayerById(playerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(playerId);
        assertThat(response.getBody().getName()).isEqualTo("Lionel Messi");
    }

    @Test
    @DisplayName("GET /futbix/v1/players/{id} not found -> ResourceNotFoundException")
    void getPlayerById_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(playerService.getPlayerById(unknownId))
                .thenThrow(new ResourceNotFoundException("Player not found with id: " + unknownId));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getPlayerById(unknownId)
        );
    }

    @Test
    @DisplayName("PUT /futbix/v1/players/{id} -> 200 with updated player")
    void updatePlayer_validRequest_returnsOk() {
        PlayerResponse updatedResponse = PlayerResponse.builder()
                .id(playerId)
                .name("Lionel Messi Updated")
                .goals(35)
                .position(Position.FORWARD)
                .age(37)
                .assists(20)
                .matches(40)
                .valueMarket(55_000_000)
                .teamName("FC Barcelona")
                .build();

        when(playerService.updatePlayer(eq(playerId), any(UpdatePlayerRequest.class)))
                .thenReturn(updatedResponse);

        ResponseEntity<PlayerResponse> response = controller.updatePlayer(playerId, updateRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Lionel Messi Updated");
        assertThat(response.getBody().getGoals()).isEqualTo(35);
    }

    @Test
    @DisplayName("PUT /futbix/v1/players/{id} not found -> ResourceNotFoundException")
    void updatePlayer_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(playerService.updatePlayer(eq(unknownId), any(UpdatePlayerRequest.class)))
                .thenThrow(new ResourceNotFoundException("Player not found with id: " + unknownId));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.updatePlayer(unknownId, updateRequest)
        );
    }

    @Test
    @DisplayName("GET /futbix/v1/players/efficiency/{id} -> 200 with efficiency response")
    void getEfficiency_returnsOk() {
        PlayerEfficiencyResponse efficiencyResponse = PlayerEfficiencyResponse.builder()
                .playerId(playerId)
                .playerName("Lionel Messi")
                .contributionsPerMatch(1.29)
                .build();
        when(playerService.getEfficiency(playerId)).thenReturn(efficiencyResponse);

        ResponseEntity<PlayerEfficiencyResponse> response = controller.getEfficiency(playerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPlayerId()).isEqualTo(playerId);
        assertThat(response.getBody().getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(response.getBody().getContributionsPerMatch()).isEqualTo(1.29);
    }

    @Test
    @DisplayName("GET /futbix/v1/players/efficiency/{id} not found -> ResourceNotFoundException")
    void getEfficiency_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(playerService.getEfficiency(unknownId))
                .thenThrow(new ResourceNotFoundException("Player not found with id: " + unknownId));

        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.getEfficiency(unknownId)
        );
    }

    @Test
    @DisplayName("DELETE /futbix/v1/players/{id} -> 204 No Content")
    void deletePlayer_returnsNoContent() {
        ResponseEntity<Void> response = controller.deletePlayer(playerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(playerService).deletePlayer(playerId);
    }
}
