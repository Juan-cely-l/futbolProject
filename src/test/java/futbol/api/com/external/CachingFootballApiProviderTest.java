package futbol.api.com.external;

import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingFootballApiProvider Unit Tests")
class CachingFootballApiProviderTest {

    @Mock
    private FootballApiProvider delegate;

    private ObjectMapper objectMapper;
    private CachingFootballApiProvider provider;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new CachingFootballApiProvider(delegate, objectMapper, tempDir, Duration.ofHours(24));
    }

    @Test
    @DisplayName("getTeamsByLeague: cache hit returns cached data without calling delegate")
    void cacheHit_returnsCachedData() throws IOException {
        List<TeamData> expected = List.of(new TeamData(1, "Team A", "Country A"));
        objectMapper.writeValue(tempDir.resolve("teams_39_2024.json").toFile(), expected);

        List<TeamData> result = provider.getTeamsByLeague(39, 2024);

        assertThat(result).containsExactlyElementsOf(expected);
        verify(delegate, never()).getTeamsByLeague(any(), any());
    }

    @Test
    @DisplayName("getTeamsByLeague: cache miss fetches from delegate and writes cache file")
    void cacheMiss_fetchesAndCaches() {
        List<TeamData> expected = List.of(
                new TeamData(1, "Team A", "Country A"),
                new TeamData(2, "Team B", "Country B")
        );
        when(delegate.getTeamsByLeague(39, 2024)).thenReturn(expected);

        List<TeamData> result = provider.getTeamsByLeague(39, 2024);

        assertThat(result).containsExactlyElementsOf(expected);
        verify(delegate).getTeamsByLeague(39, 2024);
        assertThat(tempDir.resolve("teams_39_2024.json")).exists();
    }

    @Test
    @DisplayName("getTeamsByLeague: empty response is not cached")
    void emptyResponse_notCached() {
        when(delegate.getTeamsByLeague(39, 2024)).thenReturn(List.of());

        List<TeamData> first = provider.getTeamsByLeague(39, 2024);
        assertThat(first).isEmpty();

        List<TeamData> second = provider.getTeamsByLeague(39, 2024);
        assertThat(second).isEmpty();

        verify(delegate, times(2)).getTeamsByLeague(39, 2024);
        assertThat(tempDir.resolve("teams_39_2024.json")).doesNotExist();
    }

    @Test
    @DisplayName("getTeamsByLeague: expired cache refetches from delegate")
    void expiredCache_refetches() throws IOException {
        // Write a cache file with a timestamp 25 hours ago
        Path cacheFile = tempDir.resolve("teams_39_2024.json");
        List<TeamData> oldData = List.of(new TeamData(1, "Stale", "Old Country"));
        objectMapper.writeValue(cacheFile.toFile(), oldData);
        Files.setLastModifiedTime(cacheFile,
                FileTime.from(Instant.now().minus(Duration.ofHours(25))));

        List<TeamData> freshData = List.of(new TeamData(1, "Fresh", "New Country"));
        when(delegate.getTeamsByLeague(39, 2024)).thenReturn(freshData);

        List<TeamData> result = provider.getTeamsByLeague(39, 2024);

        assertThat(result).containsExactlyElementsOf(freshData);
        verify(delegate).getTeamsByLeague(39, 2024);
    }

    @Test
    @DisplayName("getTeamsByLeague: corrupt cache file is deleted and refetched")
    void corruptCache_deletesAndRefetches() throws IOException {
        Path cacheFile = tempDir.resolve("teams_39_2024.json");
        Files.writeString(cacheFile, "this is not valid json");

        List<TeamData> expected = List.of(new TeamData(1, "Recovered", "Country"));
        when(delegate.getTeamsByLeague(39, 2024)).thenReturn(expected);

        List<TeamData> result = provider.getTeamsByLeague(39, 2024);

        assertThat(result).containsExactlyElementsOf(expected);
        verify(delegate).getTeamsByLeague(39, 2024);
        // The corrupt file should have been deleted and replaced with fresh data
        assertThat(cacheFile).exists();
    }

    @Test
    @DisplayName("getTeamsByLeague: cache write failure still returns delegate result")
    void cacheWriteFailure_gracefulDegradation() throws IOException {
        // Place a regular file where the cache directory would be created,
        // so Files.createDirectories fails with FileAlreadyExistsException.
        Path blockedCacheDir = tempDir.resolve("blocked-dir");
        Files.createFile(blockedCacheDir);

        CachingFootballApiProvider failingProvider = new CachingFootballApiProvider(
                delegate, objectMapper, blockedCacheDir, Duration.ofHours(24)
        );

        List<TeamData> expected = List.of(new TeamData(1, "Team", "Country"));
        when(delegate.getTeamsByLeague(39, 2024)).thenReturn(expected);

        List<TeamData> result = failingProvider.getTeamsByLeague(39, 2024);

        assertThat(result).containsExactlyElementsOf(expected);
        verify(delegate).getTeamsByLeague(39, 2024);
    }

    @Test
    @DisplayName("getPlayersByTeam: cache hit returns cached data without calling delegate")
    void getPlayersByTeam_cacheHit() throws IOException {
        List<PlayerInfo> expected = List.of(
                new PlayerInfo(1, "Player One", 25, "Midfielder", "photo1.jpg", 10, 5, 20)
        );
        objectMapper.writeValue(tempDir.resolve("players_50_2024_39.json").toFile(), expected);

        List<PlayerInfo> result = provider.getPlayersByTeam(50, 2024, 39);

        assertThat(result).containsExactlyElementsOf(expected);
        verify(delegate, never()).getPlayersByTeam(any(), any(), any());
    }
}
