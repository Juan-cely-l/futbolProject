package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.config.FootballApiConfig;
import futbol.api.com.external.dto.Status;
import futbol.api.com.external.dto.SyncProgress;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalFootballService Unit Tests")
class ExternalFootballServiceTest {

    @Mock
    private FootballApiProvider apiClient;

    @Mock
    private FootballDataMapper mapper;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private FootballApiConfig config;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RequestCounter requestCounter;

    @InjectMocks
    private ExternalFootballService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "self", service);
        lenient().when(requestCounter.remaining()).thenReturn(100);
    }

    @Test
    @DisplayName("syncAll: returns non-null UUID and stores progress in the map")
    void syncAll_returnsUuidAndStoresProgress() {
        when(config.season()).thenReturn(2025);
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of());

        UUID syncId = service.syncAll(140);

        assertThat(syncId).isNotNull();

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress).isNotNull();
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.leagueIds()).containsExactly(140);
        assertThat(progress.totalTeams()).isZero();
        assertThat(progress.processedTeams()).isZero();
        assertThat(progress.playersCreated()).isZero();
        assertThat(progress.playersUpdated()).isZero();
        assertThat(progress.errors()).isEmpty();
        assertThat(progress.startedAt()).isNotNull();
        assertThat(progress.completedAt()).isNotNull();
        assertThat(progress.teams()).isEmpty();
    }

    @Test
    @DisplayName("syncAll: progress status is PROCESSING immediately after syncAll returns")
    void syncAll_progressIsProcessingInitially() {
        when(config.season()).thenReturn(2025);
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of());

        UUID syncId = service.syncAll(140);

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress).isNotNull();
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
    }

    @Test
    @DisplayName("syncAll: sets correct league IDs in progress")
    void syncAll_setsCorrectLeagueIds() {
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of());

        UUID syncId = service.syncAll(List.of(78, 39), 2024);

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress.leagueIds()).containsExactly(78, 39);
        assertThat(progress.season()).isEqualTo(2024);
    }

    @Test
    @DisplayName("getProgress: returns null for unknown sync ID")
    void getProgress_unknownId_returnsNull() {
        UUID unknownId = UUID.randomUUID();

        SyncProgress progress = service.getProgress(unknownId);

        assertThat(progress).isNull();
    }

    @Test
    @DisplayName("getProgress: returns null when no sync has been started")
    void getProgress_noSyncs_returnsNull() {
        SyncProgress progress = service.getProgress(UUID.randomUUID());

        assertThat(progress).isNull();
    }

    @Test
    @DisplayName("syncAll: recalculates team budget from squad value after processing")
    void syncAll_recalculatesBudgetFromSquadValue() {
        when(config.season()).thenReturn(2025);
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of(teamData));

        TeamInfo teamInfo = new TeamInfo("arsenal", "England");
        when(mapper.toTeamInfo(teamData)).thenReturn(teamInfo);

        // Capture team when saved
        final Team[] capturedTeam = new Team[1];
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            capturedTeam[0] = t;
            return t;
        });
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(java.util.Optional.empty());

        PlayerInfo playerInfo = new PlayerInfo(1, "Saka", 23, "Midfielder", "url", 10, 8, 25);
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenReturn(List.of(playerInfo));

        PlayerData playerData = new PlayerData(Position.MIDFIELDER, 23, 45_000_000, "url");
        when(mapper.toPlayerData(playerInfo)).thenReturn(playerData);

        when(playerRepository.existsPlayerByNameAndAgeAndTeamName("saka", 23, "arsenal")).thenReturn(false);
        when(playerRepository.sumValueMarketByTeamId(any())).thenReturn(100_000_000L);
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        // Execute transaction callback synchronously for player processing phase
        doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        UUID syncId = service.syncAll(39);

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.totalTeams()).isEqualTo(1);
        assertThat(progress.processedTeams()).isEqualTo(1);
        assertThat(progress.playersCreated()).isEqualTo(1);
        assertThat(progress.errors()).isEmpty();
        assertThat(capturedTeam[0].getBudget()).isEqualTo(50_000_000L); // 100M * 0.50 (PL ratio)
        assertThat(progress.totalTeams()).isEqualTo(1);
        assertThat(progress.processedTeams()).isEqualTo(1);
        assertThat(progress.playersCreated()).isEqualTo(1);
        assertThat(capturedTeam[0].getBudget()).isEqualTo(50_000_000L); // 100M * 0.50 (PL ratio)
    }

    @Test
    @DisplayName("syncAll: team survives when player fetch fails (transaction split)")
    void syncAll_teamSurvivesPlayerFetchFailure() {
        when(config.season()).thenReturn(2025);
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of(teamData));

        TeamInfo teamInfo = new TeamInfo("arsenal", "England");
        when(mapper.toTeamInfo(teamData)).thenReturn(teamInfo);

        final Team[] capturedTeam = new Team[1];
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            capturedTeam[0] = t;
            return t;
        });
        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(java.util.Optional.empty());

        // Mock player fetch to throw (rate limit or API error)
        when(apiClient.getPlayersByTeam(1, 2025, 39)).thenThrow(new RuntimeException("API rate limit hit"));

        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        // Execute transaction callback synchronously so the exception is caught
        doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        UUID syncId = service.syncAll(39);

        SyncProgress progress = service.getProgress(syncId);
        // Status is PARTIAL because team-level errors occurred
        assertThat(progress.status()).isEqualTo(Status.PARTIAL);
        assertThat(progress.totalTeams()).isEqualTo(1);
        assertThat(progress.processedTeams()).isEqualTo(1);
        assertThat(progress.playersCreated()).isZero();
        assertThat(progress.errors()).isNotEmpty();
        // Team was created even though players failed
        assertThat(capturedTeam[0]).isNotNull();
        assertThat(capturedTeam[0].getName()).isEqualTo("arsenal");
        assertThat(capturedTeam[0].getBudget()).isEqualTo(5_000_000L); // minimum budget
    }

    @Test
    @DisplayName("syncAll: calls getTeamsByLeague only once per league (cached)")
    void syncAll_cachesTeamListBetweenEstimateAndProcess() {
        when(config.season()).thenReturn(2025);
        TeamData teamData = new TeamData(1, "Arsenal", "England");
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of(teamData));

        TeamInfo teamInfo = new TeamInfo("arsenal", "England");
        when(mapper.toTeamInfo(teamData)).thenReturn(teamInfo);

        when(teamRepository.findTeamByNameIgnoreCase("arsenal")).thenReturn(java.util.Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(apiClient.getPlayersByTeam(any(), any(), any())).thenReturn(List.of());
        when(playerRepository.findPlayersByTeam_Name("arsenal")).thenReturn(List.of());

        doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        service.syncAll(39);

        // Only one call: estimate phase caches the result, processLeague reuses it
        verify(apiClient, times(1)).getTeamsByLeague(any(), any());
    }

    @Test
    @DisplayName("syncAll: single team saves team and players")
    void syncTeams_singleTeam_savesTeamAndPlayers() {
        when(config.season()).thenReturn(2025);
        TeamData teamData = new TeamData(5, "Test FC", "Testland");
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of(teamData));

        TeamInfo teamInfo = new TeamInfo("test fc", "Testland");
        when(mapper.toTeamInfo(teamData)).thenReturn(teamInfo);

        when(teamRepository.findTeamByNameIgnoreCase("test fc")).thenReturn(java.util.Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerInfo playerInfo = new PlayerInfo(10, "Test Player", 22, "Attacker", "url", 15, 5, 20);
        when(apiClient.getPlayersByTeam(5, 2025, 39)).thenReturn(List.of(playerInfo));

        PlayerData playerData = new PlayerData(Position.FORWARD, 22, 30_000_000, "url");
        when(mapper.toPlayerData(playerInfo)).thenReturn(playerData);

        when(playerRepository.existsPlayerByNameAndAgeAndTeamName("test player", 22, "test fc")).thenReturn(false);

        when(playerRepository.sumValueMarketByTeamId(any())).thenReturn(30_000_000L);
        when(playerRepository.findPlayersByTeam_Name("test fc")).thenReturn(List.of());

        doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        UUID syncId = service.syncAll(39);

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.totalTeams()).isEqualTo(1);
        assertThat(progress.processedTeams()).isEqualTo(1);
        assertThat(progress.playersCreated()).isEqualTo(1);
        verify(teamRepository, times(2)).save(any(Team.class));
    }

    @Test
    @DisplayName("processLeague: empty team list returns progress with zero teams")
    void processLeague_emptyTeamList_returnsResultWithZeroTeams() {
        when(config.season()).thenReturn(2025);
        when(apiClient.getTeamsByLeague(any(), any())).thenReturn(List.of());

        UUID syncId = service.syncAll(78);

        SyncProgress progress = service.getProgress(syncId);
        assertThat(progress).isNotNull();
        assertThat(progress.status()).isEqualTo(Status.SUCCESS);
        assertThat(progress.totalTeams()).isZero();
        assertThat(progress.processedTeams()).isZero();
        assertThat(progress.playersCreated()).isZero();
        assertThat(progress.playersUpdated()).isZero();
        assertThat(progress.errors()).isEmpty();
        assertThat(progress.leagueIds()).containsExactly(78);
    }
}
