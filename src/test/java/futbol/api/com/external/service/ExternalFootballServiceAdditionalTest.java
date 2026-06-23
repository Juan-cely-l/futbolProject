package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.config.FootballApiConfig;
import futbol.api.com.external.dto.LeagueInfo;
import futbol.api.com.external.dto.SeasonsResponse;
import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.mapper.FootballDataMapper;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalFootballService Additional Tests")
class ExternalFootballServiceAdditionalTest {

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
    @Mock
    private RequestCounter requestCounter;

    private ExternalFootballService service;
    private FootballApiConfig config;

    @BeforeEach
    void setUp() {
        config = createConfig(2025, List.of(39, 140), 2020, 2025);
        service = new ExternalFootballService(
                apiClient, mapper, teamRepository, playerRepository, config, transactionTemplate, requestCounter);
        lenient().when(requestCounter.remaining()).thenReturn(100);
    }

    @Test
    @DisplayName("getAvailableLeagues: returns league info from config IDs")
    void getAvailableLeagues_returnsLeagueInfos() {
        List<LeagueInfo> leagues = service.getAvailableLeagues();

        assertThat(leagues).hasSize(2);
        assertThat(leagues.get(0).id()).isEqualTo(39);
        assertThat(leagues.get(0).name()).isEqualTo("Premier League");
        assertThat(leagues.get(1).id()).isEqualTo(140);
        assertThat(leagues.get(1).name()).isEqualTo("La Liga");
    }

    @Test
    @DisplayName("getAvailableLeagues: returns fallback name for unknown league ID")
    void getAvailableLeagues_unknownLeague_fallbackName() {
        config = createConfig(2025, List.of(999), 2020, 2025);
        service = new ExternalFootballService(
                apiClient, mapper, teamRepository, playerRepository, config, transactionTemplate, requestCounter);

        List<LeagueInfo> leagues = service.getAvailableLeagues();

        assertThat(leagues).hasSize(1);
        assertThat(leagues.get(0).id()).isEqualTo(999);
        assertThat(leagues.get(0).name()).isEqualTo("League 999");
    }

    @Test
    @DisplayName("getAvailableSeasons: returns seasons from config")
    void getAvailableSeasons_returnsSeasons() {
        SeasonsResponse resp = service.getAvailableSeasons();

        assertThat(resp.minSeason()).isEqualTo(2020);
        assertThat(resp.maxSeason()).isEqualTo(2025);
        assertThat(resp.currentSeason()).isEqualTo(2025);
    }

    @Test
    @DisplayName("evictStaleProgress: removes entries older than 30 minutes")
    void evictStaleProgress_removesOldEntries() {
        UUID staleId = addProgressEntry(LocalDateTime.now().minusMinutes(60));
        assertThat(service.getProgress(staleId)).isNotNull();

        service.evictStaleProgress();

        assertThat(service.getProgress(staleId)).isNull();
    }

    @Test
    @DisplayName("evictStaleProgress: keeps recent entries")
    void evictStaleProgress_keepsRecent() {
        UUID recentId = addProgressEntry(LocalDateTime.now().minusMinutes(5));
        assertThat(service.getProgress(recentId)).isNotNull();

        service.evictStaleProgress();

        assertThat(service.getProgress(recentId)).isNotNull();
    }

    private UUID addProgressEntry(LocalDateTime completedAt) {
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, SyncProgressState> progressMap = (ConcurrentHashMap<UUID, SyncProgressState>)
                ReflectionTestUtils.getField(orchestrator(), "progressMap");
        UUID id = UUID.randomUUID();
        SyncProgressState state = new SyncProgressState(List.of(39), 2025);
        state.complete();
        ReflectionTestUtils.setField(state, "completedAt", completedAt);
        progressMap.put(id, state);
        return id;
    }

    private SyncOrchestrator orchestrator() {
        return (SyncOrchestrator) ReflectionTestUtils.getField(service, "orchestrator");
    }

    private static FootballApiConfig createConfig(Integer season, List<Integer> leagueIds, Integer seasonMin, Integer seasonMax) {
        return new FootballApiConfig(
                "test-key",
                "test-host",
                "https://api.example.com",
                season,
                seasonMin,
                seasonMax,
                500,
                leagueIds);
    }
}
