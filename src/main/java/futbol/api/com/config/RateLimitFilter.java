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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MS = 60_000L;

    private final int maxRequests;
    private final List<String> trustedProxies;
    private final WindowRateLimiter rateLimiter;

    public RateLimitFilter(
            int maxRequests,
            String trustedProxiesCsv
    ) {
        this.maxRequests = maxRequests;
        this.trustedProxies = trustedProxiesCsv.isBlank()
                ? List.of()
                : Arrays.stream(trustedProxiesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        this.rateLimiter = new WindowRateLimiter(maxRequests, WINDOW_MS);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        rateLimiter.evictStaleEntries();

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;
        String ip = resolveIp(httpReq);

        if (!rateLimiter.tryAcquire(ip)) {
            long retryAfterSec = Math.max(1, rateLimiter.getTimeUntilReset(ip) / 1000);
            httpRes.setHeader("Retry-After", String.valueOf(retryAfterSec));
            log.warn("Rate limit exceeded for IP {} (max {})", ip, maxRequests);
            httpRes.setStatus(429);
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !trustedProxies.isEmpty()) {
            // Only trust X-Forwarded-For if the direct connection is from a trusted proxy
            String remoteAddr = request.getRemoteAddr();
            if (remoteAddr != null && trustedProxies.contains(remoteAddr)) {
                String[] ips = forwarded.split(",");
                return ips[0].trim();
            }
        }
        // Fall back to direct remote address when no trusted proxies are configured
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
