package futbol.api.com.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Trusted Proxy Tests")
class RateLimitFilterTrustedProxyTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Test
    @DisplayName("Trusted proxy: uses X-Forwarded-For when last proxy is trusted")
    void trustedProxy_usesForwardedFor() throws Exception {
        var filter = new RateLimitFilter(100, "proxy1,proxy2");

        when(request.getRemoteAddr()).thenReturn("proxy1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1, proxy2");

        for (int i = 0; i < 100; i++) {
            filter.doFilter(request, response, chain);
        }
        // 101st from same forwarded IP should be blocked
        filter.doFilter(request, response, chain);

        verify(chain, times(100)).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    @DisplayName("null remoteAddr: falls back to 'unknown'")
    void nullRemoteAddr_fallsBackToUnknown() throws Exception {
        var filter = new RateLimitFilter(100, "");

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
