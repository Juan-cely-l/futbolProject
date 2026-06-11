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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    @DisplayName("getAvailableLeagues: returns league info from config IDs")
    void getAvailableLeagues_returnsLeagueInfos() {
        when(config.leagueIds()).thenReturn(List.of(39, 140));

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
        when(config.leagueIds()).thenReturn(List.of(999));

        List<LeagueInfo> leagues = service.getAvailableLeagues();

        assertThat(leagues).hasSize(1);
        assertThat(leagues.get(0).id()).isEqualTo(999);
        assertThat(leagues.get(0).name()).isEqualTo("League 999");
    }

    @Test
    @DisplayName("getAvailableSeasons: returns seasons from config")
    void getAvailableSeasons_returnsSeasons() {
        when(config.seasonMin()).thenReturn(2020);
        when(config.seasonMax()).thenReturn(2025);
        when(config.season()).thenReturn(2025);

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
        var progressMap = (java.util.concurrent.ConcurrentHashMap<UUID, Object>)
                ReflectionTestUtils.getField(service, "progressMap");
        UUID id = UUID.randomUUID();
        Object stats = buildSyncStats(completedAt);
        progressMap.put(id, stats);
        return id;
    }

    private Object buildSyncStats(LocalDateTime completedAt) {
        try {
            var statsClass = Class.forName(
                    "futbol.api.com.external.service.ExternalFootballService$SyncStats");
            var ctor = statsClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Object stats = ctor.newInstance(List.of(39), 2025);
            ReflectionTestUtils.setField(stats, "completedAt", completedAt);
            // Also set status to SUCCESS so getProgress builds the result properly
            ReflectionTestUtils.setField(stats, "status",
                    futbol.api.com.external.dto.Status.SUCCESS);
            return stats;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test SyncStats", e);
        }
    }
}
