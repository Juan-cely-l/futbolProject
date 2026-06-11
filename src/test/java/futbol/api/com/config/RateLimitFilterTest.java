package futbol.api.com.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(100, "");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    @DisplayName("Under rate limit: request proceeds to chain")
    void underLimit_proceedsToChain() throws Exception {
        for (int i = 0; i < 99; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(chain, times(99)).doFilter(request, response);
    }

    @Test
    @DisplayName("Over rate limit: returns 429")
    void overLimit_returns429() throws Exception {
        for (int i = 0; i < 100; i++) {
            filter.doFilter(request, response, chain);
        }

        // The 101st request should be blocked
        filter.doFilter(request, response, chain);

        verify(chain, times(100)).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    @DisplayName("Different IPs have independent counters")
    void differentIps_independentCounters() throws Exception {
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        when(req1.getRemoteAddr()).thenReturn("192.168.1.1");
        when(req1.getHeader("X-Forwarded-For")).thenReturn(null);

        HttpServletRequest req2 = mock(HttpServletRequest.class);
        when(req2.getRemoteAddr()).thenReturn("10.0.0.1");
        when(req2.getHeader("X-Forwarded-For")).thenReturn(null);

        // Exhaust req1
        for (int i = 0; i < 100; i++) {
            filter.doFilter(req1, response, chain);
        }

        // req2 should still work
        filter.doFilter(req2, response, chain);
        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    @DisplayName("X-Forwarded-For header is used when present")
    void xForwardedFor_usedWhenPresent() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        for (int i = 0; i < 100; i++) {
            filter.doFilter(request, response, chain);
        }

        // 101st from same forwarded IP should be blocked
        filter.doFilter(request, response, chain);
        verify(response).setStatus(429);
    }
}
