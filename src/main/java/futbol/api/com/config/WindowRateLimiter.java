package futbol.api.com.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WindowRateLimiter {

    private final int maxAttempts;
    private final long windowMs;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public WindowRateLimiter(int maxAttempts, long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /**
     * Attempts to acquire a permit for the given key.
     * @return true if within limit, false if rate-limited
     */
    public boolean tryAcquire(String key) {
        Entry entry = entries.computeIfAbsent(key, k -> new Entry());
        synchronized (entry) {
            long now = System.currentTimeMillis();
            if (now - entry.windowStart > windowMs) {
                entry.windowStart = now;
                entry.count.set(1);
                return true;
            }
            int attempts = entry.count.incrementAndGet();
            return attempts <= maxAttempts;
        }
    }

    /**
     * Returns milliseconds until the window resets for the given key.
     */
    public long getTimeUntilReset(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return 0;
        synchronized (entry) {
            long now = System.currentTimeMillis();
            long elapsed = now - entry.windowStart;
            if (elapsed > windowMs) return 0;
            return windowMs - elapsed;
        }
    }

    /**
     * Removes entries that have expired beyond twice the window duration.
     */
    public void evictStaleEntries() {
        long now = System.currentTimeMillis();
        entries.values().removeIf(e -> now - e.windowStart > windowMs * 2);
    }

    private static class Entry {
        long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
