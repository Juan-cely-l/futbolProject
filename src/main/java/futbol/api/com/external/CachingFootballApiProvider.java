package futbol.api.com.external;

import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Thread-safe caching decorator for {@link FootballApiProvider}.
 *
 * <p>Caches API responses to local JSON files, using a decorator pattern so the
 * original client is unaware of the cache. Features:
 *
 * <ul>
 *   <li>{@link ReentrantReadWriteLock} per cache key (concurrent readers, exclusive writer)</li>
 *   <li>Atomic writes via {@code .tmp} + {@link java.nio.file.Files#move}</li>
 *   <li>Configurable TTL via file modification time</li>
 *   <li>Graceful degradation: corrupt files are deleted and refetched; write failures
 *       log a warning but return the live result</li>
 *   <li>Empty responses are never cached</li>
 *   <li>Exceptions from the delegate propagate unmodified</li>
 * </ul>
 */
@Slf4j
public class CachingFootballApiProvider implements FootballApiProvider {

    private final FootballApiProvider delegate;
    private final ObjectMapper objectMapper;
    private final Path cacheDir;
    private final Duration ttl;
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public CachingFootballApiProvider(
            FootballApiProvider delegate,
            ObjectMapper objectMapper,
            Path cacheDir,
            Duration ttl
    ) {
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.cacheDir = cacheDir;
        this.ttl = ttl;
    }

    @Override
    public List<TeamData> getTeamsByLeague(Integer leagueId, Integer season) {
        String cacheKey = String.format("teams_%d_%d.json", leagueId, season);
        return getCachedOrFetch(cacheKey, () -> delegate.getTeamsByLeague(leagueId, season), TeamData.class);
    }

    @Override
    public List<PlayerInfo> getPlayersByTeam(Integer teamId, Integer season, Integer leagueId) {
        String cacheKey = String.format("players_%d_%d_%d.json", teamId, season, leagueId);
        return getCachedOrFetch(cacheKey, () -> delegate.getPlayersByTeam(teamId, season, leagueId), PlayerInfo.class);
    }

    // ------------------------------------------------------------------
    // Internal: cache-or-fetch logic
    // ------------------------------------------------------------------

    /**
     * Attempts a read-lock lookup first; on miss, acquires a write-lock,
     * double-checks the cache, and only then calls the delegate.
     */
    private <T> List<T> getCachedOrFetch(String cacheKey, Supplier<List<T>> fetcher, Class<T> elementType) {
        Path cacheFile = cacheDir.resolve(cacheKey);
        ReentrantReadWriteLock lock = locks.computeIfAbsent(cacheKey, k -> new ReentrantReadWriteLock());

        // Fast path: read-lock lookup
        lock.readLock().lock();
        try {
            List<T> cached = readCacheIfValid(cacheFile, elementType);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Miss: exclusive write-lock
        lock.writeLock().lock();
        try {
            // Double-check after acquiring the write lock
            List<T> cached = readCacheIfValid(cacheFile, elementType);
            if (cached != null) {
                return cached;
            }

            // Fetch from the real API client
            List<T> result = fetcher.get();

            // Only cache non-empty responses
            if (result != null && !result.isEmpty()) {
                writeCache(cacheFile, result);
            }

            return result != null ? result : Collections.emptyList();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks whether a cache file exists, is within TTL, and can be deserialised.
     * Returns {@code null} on any failure (missing, expired, corrupt).
     */
    private <T> List<T> readCacheIfValid(Path cacheFile, Class<T> elementType) {
        if (!Files.exists(cacheFile)) {
            return null;
        }
        try {
            long lastModified = Files.getLastModifiedTime(cacheFile).toMillis();
            if (Duration.ofMillis(System.currentTimeMillis() - lastModified).compareTo(ttl) > 0) {
                log.debug("Cache expired: {}", cacheFile);
                return null;
            }
        } catch (IOException e) {
            log.warn("Cannot read last-modified time for {}; treating as miss", cacheFile);
            return null;
        }

        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            List<T> result = objectMapper.readValue(cacheFile.toFile(), type);
            if (result == null || result.isEmpty()) {
                return null;
            }
            return result;
        } catch (Exception e) {
            log.warn("Corrupt cache file {}, deleting and refetching", cacheFile, e);
            deleteQuietly(cacheFile);
            return null;
        }
    }

    /**
     * Atomically writes data to a cache file. Writes to a {@code .tmp} sibling
     * first, then performs an atomic {@link Files#move move}.
     */
    private <T> void writeCache(Path cacheFile, List<T> data) {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            log.warn("Cannot create cache directory {}, skipping write", cacheDir, e);
            return;
        }

        Path tmpFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
        try {
            objectMapper.writeValue(tmpFile.toFile(), data);
            Files.move(tmpFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Cached {} entries to {}", data.size(), cacheFile);
        } catch (IOException e) {
            log.warn("Failed to write cache file {}, falling back to live fetch", cacheFile, e);
            deleteQuietly(tmpFile);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
