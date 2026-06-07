package futbol.api.com.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MS = 60_000L;
    private static final long EVICTION_INTERVAL_MS = 300_000L;

    private final int maxRequests;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private volatile long lastEviction = System.currentTimeMillis();

    public RateLimitFilter(@Value("${rate-limit.max-requests:100}") int maxRequests) {
        this.maxRequests = maxRequests;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Periodically evict stale entries to prevent memory leaks
        evictStaleEntries();

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String ip = resolveIp(httpReq);
        WindowCounter counter = counters.computeIfAbsent(ip, k -> new WindowCounter());

        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStart > WINDOW_MS) {
                counter.windowStart = now;
                counter.count.set(1);
            } else {
                int current = counter.count.incrementAndGet();
                if (current > maxRequests) {
                    log.warn("Rate limit exceeded for IP {}: {} requests in current window (max {})", ip, current, maxRequests);
                    ((HttpServletResponse) response).setStatus(429);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void evictStaleEntries() {
        long now = System.currentTimeMillis();
        if (now - lastEviction > EVICTION_INTERVAL_MS) {
            lastEviction = now;
            counters.values().removeIf(c -> now - c.windowStart > WINDOW_MS * 2);
        }
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private static class WindowCounter {
        long windowStart = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }
}
