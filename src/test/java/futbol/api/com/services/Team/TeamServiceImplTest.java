package futbol.api.com.services.Team;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.dtos.team.CreateTeamRequest;
import futbol.api.com.dtos.team.TeamResponse;
import futbol.api.com.dtos.team.TeamValueResponse;
import futbol.api.com.dtos.team.UpdateTeamRequest;
import futbol.api.com.exceptions.ResourceAlreadyExistsException;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import futbol.api.com.services.Player.PlayerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("TeamServiceImpl Unit Tests")
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    private final UUID teamId = UUID.randomUUID();
    private final Team team = Team.builder()
            .id(teamId)
            .name("FC Barcelona")
            .budget(500_000_000L)
            .city("Barcelona")
            .build();

    // -----------------------------------------------------------------------
    // getTeamSquad
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTeamSquad: returns player list when team exists")
    void getTeamSquad_whenTeamExists_returnsPlayerList() {
        String teamName = "FC Barcelona";

        Player player = Player.builder()
                .id(UUID.randomUUID())
                .name("lionel messi").goals(25).position(Position.FORWARD)
                .age(33).assists(10).matches(30).valueMarket(120_000_000)
                .team(team).build();

        PlayerResponse response = PlayerResponse.builder()
                .id(player.getId()).name("lionel messi").goals(25)
                .position(Position.FORWARD).age(33).assists(10)
                .matches(30).valueMarket(120_000_000).teamName(teamName)
                .build();

        when(teamRepository.existsByNameIgnoreCase(teamName)).thenReturn(true);
        when(playerRepository.findPlayersByTeam_Name(teamName)).thenReturn(List.of(player));
        when(playerMapper.mapPlayerToResponseDto(player)).thenReturn(response);

        List<PlayerResponse> result = teamService.getTeamSquad(teamName);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("lionel messi");
        assertThat(result.get(0).getTeamName()).isEqualTo(teamName);
        verify(teamRepository).existsByNameIgnoreCase(teamName);
        verify(playerRepository).findPlayersByTeam_Name(teamName);
    }

    @Test
    @DisplayName("getTeamSquad: throws ResourceNotFoundException when team does not exist")
    void getTeamSquad_whenTeamDoesNotExist_throwsException() {
        String teamName = "NonExistent";
        when(teamRepository.existsByNameIgnoreCase(teamName)).thenReturn(false);

        assertThatThrownBy(() -> teamService.getTeamSquad(teamName))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(teamName);

        verify(playerRepository, never()).findPlayersByTeam_Name(any());
    }

    // -----------------------------------------------------------------------
    // getTeamValue
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTeamValue: returns formatted squad value when team exists")
    void getTeamValue_whenTeamExists_returnsFormattedValue() {
        String teamName = "FC Barcelona";
        when(teamRepository.findTeamByNameIgnoreCase(teamName)).thenReturn(Optional.of(team));
        when(playerRepository.sumValueMarketByTeamId(teamId)).thenReturn(120_000_000L);

        TeamValueResponse result = teamService.getTeamValue(teamName);

        assertThat(result.getTeamName()).isEqualTo("FC Barcelona");
        assertThat(result.getTotalValue()).isEqualTo(120_000_000L);
    }

    @Test
    @DisplayName("getTeamValue: returns $0 when sumValueMarket is null")
    void getTeamValue_whenSumIsNull_returnsZero() {
        String teamName = "FC Barcelona";
        when(teamRepository.findTeamByNameIgnoreCase(teamName)).thenReturn(Optional.of(team));
        when(playerRepository.sumValueMarketByTeamId(teamId)).thenReturn(null);

        TeamValueResponse result = teamService.getTeamValue(teamName);

        assertThat(result.getTeamName()).isEqualTo("FC Barcelona");
        assertThat(result.getTotalValue()).isZero();
    }

    @Test
    @DisplayName("getTeamValue: throws ResourceNotFoundException when team does not exist")
    void getTeamValue_whenTeamDoesNotExist_throwsException() {
        String teamName = "NonExistent";
        when(teamRepository.findTeamByNameIgnoreCase(teamName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamValue(teamName))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(teamName);
    }

    // -----------------------------------------------------------------------
    // createTeam
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createTeam: normalizes name/city, saves and returns TeamResponse")
    void createTeam_success_returnsTeamResponse() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("  Real Madrid  ");
        request.setCity("  Madrid  ");
        request.setBudget(300_000_000L);

        String normalizedName = "real madrid";
        when(teamRepository.existsByNameIgnoreCase(normalizedName)).thenReturn(false);

        Team savedTeam = Team.builder()
                .id(teamId).name(normalizedName).budget(300_000_000L).city("madrid")
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        TeamResponse result = teamService.createTeam(request);

        assertThat(result.getId()).isEqualTo(teamId);
        assertThat(result.getName()).isEqualTo(normalizedName);
        assertThat(result.getBudget()).isEqualTo(300_000_000L);
        assertThat(result.getCity()).isEqualTo("madrid");
    }

    @Test
    @DisplayName("createTeam: throws ResourceAlreadyExistsException when team name exists")
    void createTeam_whenAlreadyExists_throwsException() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("  Barcelona  ");
        request.setCity("Barcelona");
        request.setBudget(200_000_000L);

        when(teamRepository.existsByNameIgnoreCase("barcelona")).thenReturn(true);

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Barcelona");

        verify(teamRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // updateTeam
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateTeam: updates all provided fields")
    void updateTeam_withAllFields_returnsUpdatedResponse() {
        UUID id = UUID.randomUUID();
        Team existingTeam = Team.builder()
                .id(id).name("old name").budget(100L).city("old city").build();

        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("FC Barcelona Updated");
        request.setCity("Barcelona City");
        request.setBudget(600_000_000L);

        when(teamRepository.findById(id)).thenReturn(Optional.of(existingTeam));

        Team updatedTeam = Team.builder()
                .id(id).name("FC Barcelona Updated").city("Barcelona City").budget(600_000_000L)
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(updatedTeam);

        TeamResponse result = teamService.updateTeam(id, request);

        assertThat(result.getName()).isEqualTo("FC Barcelona Updated");
        assertThat(result.getCity()).isEqualTo("Barcelona City");
        assertThat(result.getBudget()).isEqualTo(600_000_000L);
    }

    @Test
    @DisplayName("updateTeam: only updates non-null fields, ignores blank name")
    void updateTeam_withPartialFields_updatesOnlyProvided() {
        UUID id = UUID.randomUUID();
        Team existingTeam = Team.builder()
                .id(id).name("FC Barcelona").city("Barcelona").budget(500_000_000L).build();

        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("   "); // blank — should be ignored
        request.setBudget(999_999_999L);
        // city is null

        when(teamRepository.findById(id)).thenReturn(Optional.of(existingTeam));

        Team updatedTeam = Team.builder()
                .id(id).name("FC Barcelona").city("Barcelona").budget(999_999_999L)
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(updatedTeam);

        TeamResponse result = teamService.updateTeam(id, request);

        assertThat(result.getName()).isEqualTo("FC Barcelona");
        assertThat(result.getCity()).isEqualTo("Barcelona");
        assertThat(result.getBudget()).isEqualTo(999_999_999L);
    }

    @Test
    @DisplayName("updateTeam: omitted name and city keep persisted values")
    void updateTeam_withOmittedStringFields_keepsPersistedValues() {
        UUID id = UUID.randomUUID();
        Team existingTeam = Team.builder()
                .id(id).name("FC Barcelona").city("Barcelona").budget(500_000_000L).build();

        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setBudget(650_000_000L);

        when(teamRepository.findById(id)).thenReturn(Optional.of(existingTeam));

        Team updatedTeam = Team.builder()
                .id(id).name("FC Barcelona").city("Barcelona").budget(650_000_000L)
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(updatedTeam);

        TeamResponse result = teamService.updateTeam(id, request);

        assertThat(result.getName()).isEqualTo("FC Barcelona");
        assertThat(result.getCity()).isEqualTo("Barcelona");
        assertThat(result.getBudget()).isEqualTo(650_000_000L);
    }

    @Test
    @DisplayName("updateTeam: throws ResourceNotFoundException when team not found")
    void updateTeam_whenTeamNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Any Name");

        when(teamRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.updateTeam(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(teamRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // getTeamById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTeamById: returns TeamResponse when found")
    void getTeamById_whenFound_returnsTeamResponse() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        TeamResponse result = teamService.getTeamById(teamId);

        assertThat(result.getId()).isEqualTo(teamId);
        assertThat(result.getName()).isEqualTo("FC Barcelona");
        assertThat(result.getBudget()).isEqualTo(500_000_000L);
        assertThat(result.getCity()).isEqualTo("Barcelona");
    }

    @Test
    @DisplayName("getTeamById: throws ResourceNotFoundException when not found")
    void getTeamById_whenNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(teamRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // -----------------------------------------------------------------------
    // getTeamByName
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getTeamByName: returns TeamResponse when found")
    void getTeamByName_whenFound_returnsTeamResponse() {
        String teamName = "FC Barcelona";
        when(teamRepository.findTeamByNameIgnoreCase(teamName)).thenReturn(Optional.of(team));

        TeamResponse result = teamService.getTeamByName(teamName);

        assertThat(result.getId()).isEqualTo(teamId);
        assertThat(result.getName()).isEqualTo(teamName);
        assertThat(result.getBudget()).isEqualTo(500_000_000L);
        assertThat(result.getCity()).isEqualTo("Barcelona");
    }

    @Test
    @DisplayName("getTeamByName: throws ResourceNotFoundException when not found")
    void getTeamByName_whenNotFound_throwsException() {
        String teamName = "NonExistent";
        when(teamRepository.findTeamByNameIgnoreCase(teamName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamByName(teamName))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(teamName);
    }

    // -----------------------------------------------------------------------
    // getAllTeams
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllTeams: returns all teams as TeamResponse list")
    void getAllTeams_withTeams_returnsList() {
        Team team2 = Team.builder()
                .id(UUID.randomUUID()).name("Real Madrid").budget(400_000_000L).city("Madrid")
                .build();

        when(teamRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(team, team2)));

        Page<TeamResponse> result = teamService.getAllTeams(0, 10, "name", "asc", null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TeamResponse::getName)
                .containsExactly("FC Barcelona", "Real Madrid");
    }

    @Test
    @DisplayName("getAllTeams: returns empty list when no teams exist")
    void getAllTeams_whenNoTeams_returnsEmptyList() {
        when(teamRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<TeamResponse> result = teamService.getAllTeams(0, 10, "name", "asc", null);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // deleteTeam
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteTeam: deletes team when exists")
    void deleteTeam_whenExists_deletesSuccessfully() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        teamService.deleteTeam(teamId);

        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("deleteTeam: throws ResourceNotFoundException when team does not exist")
    void deleteTeam_whenNotExists_throwsException() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(teamId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(teamId.toString());

        verify(teamRepository, never()).delete(any());
    }
}
