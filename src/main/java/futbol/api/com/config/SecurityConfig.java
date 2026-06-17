package futbol.api.com.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(Environment environment, RateLimitFilter rateLimitFilter) {
        this.environment = environment;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // NOSONAR: stateless REST API with HTTP Basic (no session cookies to hijack)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/futbix/v1/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(bruteForceEntryPoint()))
            .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class)
            .headers(headers -> headers
                .defaultsDisabled()
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=()"))
            );
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint bruteForceEntryPoint() {
        WindowRateLimiter authLimiter = new WindowRateLimiter(10, 300_000L);

        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.AuthenticationException authException) -> {
            String ip = request.getRemoteAddr();
            if (ip == null) ip = "unknown";

            authLimiter.evictStaleEntries();

            if (!authLimiter.tryAcquire(ip)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Too many failed authentication attempts. Try again later.\"}");
                return;
            }

            response.setHeader("WWW-Authenticate", "Basic realm=\"Futbix API\"");
            response.setStatus(401);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Authentication required\"}");
        };
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        String password = environment.getProperty("ADMIN_PASSWORD", System.getenv("ADMIN_PASSWORD"));
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD environment variable is not set. "
                    + "Set it in your .env file or environment before starting the application.");
        }
        var user = User.builder()
                .username("admin")
                .password(encoder.encode(password))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
