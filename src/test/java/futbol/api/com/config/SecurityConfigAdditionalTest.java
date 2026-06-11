package futbol.api.com.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "ADMIN_PASSWORD=test123")
@DisplayName("SecurityConfig Additional Integration Tests")
class SecurityConfigAdditionalTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationEntryPoint bruteForceEntryPoint;

    @Test
    @DisplayName("passwordEncoder: BCrypt bean is created")
    void passwordEncoder_beanCreated() {
        assertThat(passwordEncoder).isNotNull();
        String hash = passwordEncoder.encode("test");
        assertThat(hash).startsWith("$2a$");
        assertThat(passwordEncoder.matches("test", hash)).isTrue();
    }

    @Test
    @DisplayName("userDetailsService: admin user loads with correct password")
    void userDetailsService_loadsAdminUser() {
        var userDetails = userDetailsService.loadUserByUsername("admin");
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("bruteForceEntryPoint: first attempt returns 401 with WWW-Authenticate")
    void bruteForceEntryPoint_firstAttempt_returns401() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        AuthenticationException authEx = mock(AuthenticationException.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        when(res.getWriter()).thenReturn(writer);

        bruteForceEntryPoint.commence(req, res, authEx);

        verify(res).setHeader("WWW-Authenticate", "Basic realm=\"Futbix API\"");
        verify(res).setStatus(401);
        assertThat(stringWriter.toString()).contains("Authentication required");
    }

    @Test
    @DisplayName("bruteForceEntryPoint: after threshold returns 429")
    void bruteForceEntryPoint_afterThreshold_returns429() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        AuthenticationException authEx = mock(AuthenticationException.class);
        when(req.getRemoteAddr()).thenReturn("10.0.0.2");

        // First 10 attempts → 401
        for (int i = 0; i < 10; i++) {
            HttpServletResponse r = mock(HttpServletResponse.class);
            StringWriter sw = new StringWriter();
            when(r.getWriter()).thenReturn(new PrintWriter(sw));
            bruteForceEntryPoint.commence(req, r, authEx);
            verify(r).setStatus(401);
        }

        // 11th attempt → 429
        StringWriter sw = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(sw));
        bruteForceEntryPoint.commence(req, res, authEx);
        verify(res).setStatus(429);
        assertThat(sw.toString()).contains("Too many failed authentication attempts");
    }
}
