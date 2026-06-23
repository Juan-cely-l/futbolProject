package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.dto.SyncTeamResult;
import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.mapper.FootballDataMapper;
import futbol.api.com.external.mapper.FootballDataMapper.PlayerData;
import futbol.api.com.external.mapper.FootballDataMapper.TeamInfo;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamProcessor Unit Tests")
class TeamProcessorTest {

    @Mock
    private FootballApiProvider apiClient;
    @Mock
    private FootballDataMapper mapper;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    private TeamProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TeamProcessor(apiClient, mapper, teamRepository, playerRepository, transactionTemplate);
    }

    @Test
    @DisplayName("processTeam: player fetch failure does not open transaction and returns partial error")
    void processTeam_playerFetchFails_doesNotOpenPlayerTransactionAndReturnsPartialError() {
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        Team team = new Team();
        team.setName("arsenal");
        team.setCountry("England");
        team.setBudget(5_000_000L);

        when(mapper.toTeamInfo(teamData)).thenReturn(new TeamInfo("arsenal", "England"));
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenThrow(new RuntimeException("API rate limit hit"));
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        TeamProcessingResult result = processor.processTeam(teamData, 39, 2025);

        assertThat(result.playersCreated()).isZero();
        assertThat(result.playersUpdated()).isZero();
        assertThat(result.errors()).contains("Arsenal: API rate limit hit");
        assertThat(result.teamResult().name()).isEqualTo("arsenal");
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    @DisplayName("processTeam: persistence failure after fetch records error without refetching")
    void processTeam_playerPersistenceFails_afterSingleSuccessfulFetch_recordsPartialErrorWithoutRefetch() {
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        Team team = new Team();
        team.setName("arsenal");
        team.setCountry("England");

        PlayerInfo playerInfo = new PlayerInfo(10, "Saka", 23, "Midfielder", "url", 10, 8, 25);

        when(mapper.toTeamInfo(teamData)).thenReturn(new TeamInfo("arsenal", "England"));
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(Optional.of(team));
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenReturn(List.of(playerInfo));
        doThrow(new RuntimeException("database unavailable"))
                .when(transactionTemplate).executeWithoutResult(any());
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        TeamProcessingResult result = processor.processTeam(teamData, 39, 2025);

        assertThat(result.errors()).contains("Arsenal: database unavailable");
        verify(apiClient, times(1)).getPlayersByTeam(1, 2025, 39);
    }

    @Test
    @DisplayName("processTeam: new team survives player fetch failure with minimum budget")
    void processTeam_newTeamCreated_playerFetchFails_keepsTeamWithMinimumBudget() {
        TeamData teamData = new TeamData(1, "Arsenal", "England");

        when(mapper.toTeamInfo(teamData)).thenReturn(new TeamInfo("arsenal", "England"));
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenThrow(new RuntimeException("API timeout"));
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        TeamProcessingResult result = processor.processTeam(teamData, 39, 2025);

        assertThat(result.teamResult().created()).isTrue();
        assertThat(result.teamResult().updated()).isFalse();
        assertThat(result.errors()).contains("Arsenal: API timeout");
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("processTeam: existing team survives player persistence failure")
    void processTeam_existingTeam_playerPersistenceFails_keepsExistingTeam() {
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        Team team = new Team();
        team.setName("arsenal");
        team.setCountry("England");

        when(mapper.toTeamInfo(teamData)).thenReturn(new TeamInfo("arsenal", "England"));
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(Optional.of(team));
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenReturn(List.of());
        doThrow(new RuntimeException("constraint failure"))
                .when(transactionTemplate).executeWithoutResult(any());
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        TeamProcessingResult result = processor.processTeam(teamData, 39, 2025);

        assertThat(result.teamResult().created()).isFalse();
        assertThat(result.teamResult().updated()).isTrue();
        assertThat(result.errors()).contains("Arsenal: constraint failure");
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("processTeam: completed team builds stable sync team result")
    void processTeam_completedTeam_buildsStableSyncTeamResult() {
        TeamData teamData = new TeamData(5, "Test FC", "Testland");
        Team team = new Team();
        team.setName("test fc");
        team.setCountry("Testland");

        PlayerInfo playerInfo = new PlayerInfo(10, "Test Player", 22, "Attacker", "url", 15, 5, 20);
        PlayerData playerData = new PlayerData(Position.FORWARD, 22, 30_000_000, "url");
        futbol.api.com.models.Player persistedPlayer = new futbol.api.com.models.Player();
        persistedPlayer.setName("test player");
        persistedPlayer.setPosition(Position.FORWARD);
        persistedPlayer.setAge(22);
        persistedPlayer.setPhoto("url");
        persistedPlayer.setGoals(15);
        persistedPlayer.setAssists(5);
        persistedPlayer.setMatches(20);
        persistedPlayer.setValueMarket(30_000_000);

        when(mapper.toTeamInfo(teamData)).thenReturn(new TeamInfo("test fc", "Testland"));
        when(teamRepository.findTeamByNameIgnoreCase("test fc")).thenReturn(Optional.of(team));
        when(apiClient.getPlayersByTeam(5, 2025, 39)).thenReturn(List.of(playerInfo));
        when(mapper.toPlayerData(playerInfo)).thenReturn(playerData);
        when(playerRepository.existsPlayerByNameAndAgeAndTeamName("test player", 22, "test fc"))
                .thenReturn(false);
        when(playerRepository.sumValueMarketByTeamId(any())).thenReturn(30_000_000L);
        when(playerRepository.findPlayersByTeam_Name("test fc")).thenReturn(List.of(persistedPlayer));
        doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        TeamProcessingResult result = processor.processTeam(teamData, 39, 2025);

        assertThat(result.playersCreated()).isEqualTo(1);
        assertThat(result.playersUpdated()).isZero();
        SyncTeamResult teamResult = result.teamResult();
        assertThat(teamResult.name()).isEqualTo("test fc");
        assertThat(teamResult.country()).isEqualTo("Testland");
        assertThat(teamResult.players()).hasSize(1);
        assertThat(teamResult.players().get(0).name()).isEqualTo("test player");
    }
}
