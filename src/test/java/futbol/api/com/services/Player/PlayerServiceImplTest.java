package futbol.api.com.services.Player;

import futbol.api.com.dtos.player.CreatePlayerRequest;
import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.player.UpdatePlayerRequest;
import futbol.api.com.exceptions.ResourceAlreadyExistsException;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import futbol.api.com.dtos.team.PlayerEfficiencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerServiceImpl Unit Tests")
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PlayerServiceImpl playerService;

    private final UUID teamId = UUID.randomUUID();
    private final Team team = Team.builder()
            .id(teamId).name("fc barcelona").budget(500_000_000L).city("barcelona")
            .build();

    private final UUID playerId = UUID.randomUUID();
    private final Player player = Player.builder()
            .id(playerId).name("lionel messi").goals(25).position(Position.FORWARD)
            .age(33).assists(10).matches(30).valueMarket(120_000_000)
            .team(team)
            .build();

    // -----------------------------------------------------------------------
    // createPlayer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createPlayer: normalizes name, saves and returns PlayerResponse")
    void createPlayer_success_returnsPlayerResponse() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName("  Lionel Messi  ");
        request.setAge(33);
        request.setGoals(25);
        request.setPosition(Position.FORWARD);
        request.setAssists(10);
        request.setMatches(30);
        request.setValueMarket(120_000_000);
        request.setTeamName("FC Barcelona");

        when(teamRepository.findTeamByNameIgnoreCase("FC Barcelona")).thenReturn(Optional.of(team));
        when(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "lionel messi", 33, "fc barcelona")).thenReturn(false);

        Player savedPlayer = Player.builder()
                .id(playerId).name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team)
                .build();
        when(playerRepository.save(any(Player.class))).thenReturn(savedPlayer);

        PlayerResponse expected = PlayerResponse.builder()
                .id(playerId).name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .teamName("fc barcelona")
                .build();
        when(playerMapper.mapPlayerToResponseDto(savedPlayer)).thenReturn(expected);

        PlayerResponse result = playerService.createPlayer(request);

        assertThat(result.getName()).isEqualTo("lionel messi");
        assertThat(result.getTeamName()).isEqualTo("fc barcelona");
        assertThat(result.getAge()).isEqualTo(33);
        assertThat(result.getGoals()).isEqualTo(25);
        assertThat(result.getPosition()).isEqualTo(Position.FORWARD);
    }

    @Test
    @DisplayName("createPlayer: throws ResourceNotFoundException when team does not exist")
    void createPlayer_whenTeamNotFound_throwsException() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setTeamName("NonExistent Team");

        when(teamRepository.findTeamByNameIgnoreCase("NonExistent Team"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.createPlayer(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPlayer: throws ResourceAlreadyExistsException when duplicate player")
    void createPlayer_whenAlreadyExists_throwsException() {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setName("  Lionel Messi  ");
        request.setAge(33);
        request.setTeamName("FC Barcelona");

        when(teamRepository.findTeamByNameIgnoreCase("FC Barcelona")).thenReturn(Optional.of(team));
        when(playerRepository.existsPlayerByNameAndAgeAndTeamName(
                "lionel messi", 33, "fc barcelona")).thenReturn(true);

        assertThatThrownBy(() -> playerService.createPlayer(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(playerRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // updatePlayer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updatePlayer: updates all scalar fields without team change")
    void updatePlayer_withAllScalarFields_returnsUpdatedResponse() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setAge(34);
        request.setAssists(15);
        request.setMatches(35);
        request.setGoals(30);
        request.setValueMarket(150_000_000);
        // teamName is null — team stays unchanged

        UUID pid = UUID.randomUUID();
        Player existing = Player.builder()
                .id(pid).name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team)
                .build();

        when(playerRepository.findById(pid)).thenReturn(Optional.of(existing));

        Player updated = Player.builder()
                .id(pid).name("lionel messi").goals(30).position(Position.FORWARD)
                .age(34).assists(15).matches(35).valueMarket(150_000_000)
                .team(team)
                .build();
        when(playerRepository.save(any(Player.class))).thenReturn(updated);

        PlayerResponse expected = PlayerResponse.builder()
                .id(pid).name("lionel messi").goals(30).position(Position.FORWARD)
                .age(34).assists(15).matches(35).valueMarket(150_000_000)
                .teamName("fc barcelona")
                .build();
        when(playerMapper.mapPlayerToResponseDto(updated)).thenReturn(expected);

        PlayerResponse result = playerService.updatePlayer(pid, request);

        assertThat(result.getAge()).isEqualTo(34);
        assertThat(result.getGoals()).isEqualTo(30);
        assertThat(result.getAssists()).isEqualTo(15);
        assertThat(result.getMatches()).isEqualTo(35);
        assertThat(result.getValueMarket()).isEqualTo(150_000_000);
        assertThat(result.getTeamName()).isEqualTo("fc barcelona");
    }

    @Test
    @DisplayName("updatePlayer: changes team when teamName is provided")
    void updatePlayer_withTeamChange_updatesTeam() {
        Team newTeam = Team.builder()
                .id(UUID.randomUUID()).name("real madrid").budget(400_000_000L).city("madrid")
                .build();

        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setGoals(30);
        request.setTeamName("Real Madrid");

        UUID pid = UUID.randomUUID();
        Player existing = Player.builder()
                .id(pid).name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team)
                .build();

        when(playerRepository.findById(pid)).thenReturn(Optional.of(existing));
        when(teamRepository.findTeamByNameIgnoreCase("real madrid")).thenReturn(Optional.of(newTeam));

        Player updated = Player.builder()
                .id(pid).name("lionel messi").goals(30).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(newTeam)
                .build();
        when(playerRepository.save(any(Player.class))).thenReturn(updated);

        PlayerResponse expected = PlayerResponse.builder()
                .id(pid).name("lionel messi").goals(30).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .teamName("real madrid")
                .build();
        when(playerMapper.mapPlayerToResponseDto(updated)).thenReturn(expected);

        PlayerResponse result = playerService.updatePlayer(pid, request);

        assertThat(result.getTeamName()).isEqualTo("real madrid");
        assertThat(result.getGoals()).isEqualTo(30);
    }

    @Test
    @DisplayName("updatePlayer: omitted name and teamName keep persisted values")
    void updatePlayer_withOmittedStringFields_keepsPersistedValues() {
        UUID pid = UUID.randomUUID();
        Player existing = Player.builder()
                .id(pid).name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team)
                .build();

        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setGoals(40);

        when(playerRepository.findById(pid)).thenReturn(Optional.of(existing));

        Player updated = Player.builder()
                .id(pid).name("lionel messi").goals(40).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team)
                .build();
        when(playerRepository.save(any(Player.class))).thenReturn(updated);

        PlayerResponse expected = PlayerResponse.builder()
                .id(pid).name("lionel messi").goals(40).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .teamName("fc barcelona")
                .build();
        when(playerMapper.mapPlayerToResponseDto(updated)).thenReturn(expected);

        PlayerResponse result = playerService.updatePlayer(pid, request);

        assertThat(result.getName()).isEqualTo("lionel messi");
        assertThat(result.getTeamName()).isEqualTo("fc barcelona");
        assertThat(result.getGoals()).isEqualTo(40);
    }

    @Test
    @DisplayName("updatePlayer: throws ResourceNotFoundException when player not found")
    void updatePlayer_whenPlayerNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(playerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.updatePlayer(id, new UpdatePlayerRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");

        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePlayer: throws ResourceNotFoundException when new team does not exist")
    void updatePlayer_whenNewTeamNotFound_throwsException() {
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setTeamName("NonExistent Team");

        UUID pid = UUID.randomUUID();
        Player existing = Player.builder()
                .id(pid).name("player").goals(0).position(Position.MIDFIELDER)
                .age(25).assists(0).matches(0).valueMarket(0)
                .team(team)
                .build();

        when(playerRepository.findById(pid)).thenReturn(Optional.of(existing));
        when(teamRepository.findTeamByNameIgnoreCase("nonexistent team"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.updatePlayer(pid, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NonExistent Team");

        verify(playerRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // getEfficiency
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getEfficiency: calculates (goals + assists) / matches correctly")
    void getEfficiency_withNormalValues_returnsFormatted() {
        UUID pid = UUID.randomUUID();
        Player p = Player.builder()
                .id(pid).name("lionel messi").goals(20).assists(10).matches(30).team(team).build();
        when(playerRepository.findById(pid)).thenReturn(Optional.of(p));

        PlayerEfficiencyResponse result = playerService.getEfficiency(pid);

        assertThat(result.getPlayerId()).isEqualTo(pid);
        assertThat(result.getPlayerName()).isEqualTo("lionel messi");
        assertThat(result.getContributionsPerMatch()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("getEfficiency: returns 0.0 when matches is zero")
    void getEfficiency_zeroMatches_returnsZeroZero() {
        UUID pid = UUID.randomUUID();
        Player p = Player.builder()
                .id(pid).goals(5).assists(3).matches(0).team(team).build();
        when(playerRepository.findById(pid)).thenReturn(Optional.of(p));

        PlayerEfficiencyResponse result = playerService.getEfficiency(pid);

        assertThat(result.getPlayerId()).isEqualTo(pid);
        assertThat(result.getContributionsPerMatch()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getEfficiency: treats null assists and null goals as 0")
    void getEfficiency_nullAssistsAndGoals_usesZero() {
        UUID pid = UUID.randomUUID();
        Player p = Player.builder()
                .id(pid).goals(null).assists(null).matches(10).team(team).build();
        when(playerRepository.findById(pid)).thenReturn(Optional.of(p));

        PlayerEfficiencyResponse result = playerService.getEfficiency(pid);

        assertThat(result.getPlayerId()).isEqualTo(pid);
        assertThat(result.getContributionsPerMatch()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getEfficiency: returns 0.0 when matches is null")
    void getEfficiency_nullMatches_returnsZeroZero() {
        UUID pid = UUID.randomUUID();
        Player p = Player.builder()
                .id(pid).goals(5).assists(3).matches(null).team(team).build();
        when(playerRepository.findById(pid)).thenReturn(Optional.of(p));

        PlayerEfficiencyResponse result = playerService.getEfficiency(pid);

        assertThat(result.getPlayerId()).isEqualTo(pid);
        assertThat(result.getContributionsPerMatch()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getEfficiency: throws ResourceNotFoundException when player not found")
    void getEfficiency_whenPlayerNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(playerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getEfficiency(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // -----------------------------------------------------------------------
    // getAllPlayers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllPlayers: returns all players as PlayerResponse list")
    void getAllPlayers_withPlayers_returnsList() {
        Player player2 = Player.builder()
                .id(UUID.randomUUID()).name("cristiano ronaldo").goals(30)
                .position(Position.FORWARD).age(35).assists(5).matches(28)
                .valueMarket(80_000_000).team(team)
                .build();

        PlayerResponse pr1 = PlayerResponse.builder().name("lionel messi").build();
        PlayerResponse pr2 = PlayerResponse.builder().name("cristiano ronaldo").build();

        Page<Player> playerPage = new PageImpl<>(List.of(player, player2));
        when(playerRepository.findAll(any(Pageable.class))).thenReturn(playerPage);
        when(playerMapper.mapPlayerToResponseDto(player)).thenReturn(pr1);
        when(playerMapper.mapPlayerToResponseDto(player2)).thenReturn(pr2);

        Page<PlayerResponse> result = playerService.getAllPlayers(0, 10, "name", "asc", null);

        assertThat(result).hasSize(2);
        assertThat(result.getContent()).extracting(PlayerResponse::getName)
                .containsExactly("lionel messi", "cristiano ronaldo");
    }

    @Test
    @DisplayName("getAllPlayers: returns empty list when no players exist")
    void getAllPlayers_whenNoPlayers_returnsEmptyList() {
        when(playerRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<PlayerResponse> result = playerService.getAllPlayers(0, 10, "name", "asc", null);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // deletePlayer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deletePlayer: deletes existing player successfully")
    void deletePlayer_whenPlayerExists_deletesSuccessfully() {
        UUID pid = UUID.randomUUID();
        Player p = Player.builder()
                .id(pid).name("player").goals(0).position(Position.MIDFIELDER)
                .age(25).assists(0).matches(0).valueMarket(0)
                .team(team)
                .build();
        when(playerRepository.findById(pid)).thenReturn(Optional.of(p));

        playerService.deletePlayer(pid);

        verify(playerRepository).delete(p);
    }

    @Test
    @DisplayName("deletePlayer: throws ResourceNotFoundException when player not found")
    void deletePlayer_whenPlayerNotFound_throwsException() {
        UUID pid = UUID.randomUUID();
        when(playerRepository.findById(pid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.deletePlayer(pid))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(pid.toString());

        verify(playerRepository, never()).delete(any());
    }
}
