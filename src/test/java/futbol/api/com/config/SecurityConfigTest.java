package futbol.api.com.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RateLimitFilter rateLimitFilter = mock(RateLimitFilter.class);

    @Test
    @DisplayName("userDetailsService: creates user when ADMIN_PASSWORD is set")
    void userDetailsService_withPassword_createsUser() {
        Environment env = mock(Environment.class);
        when(env.getProperty("ADMIN_PASSWORD", System.getenv("ADMIN_PASSWORD"))).thenReturn("test123");

        SecurityConfig config = new SecurityConfig(env, rateLimitFilter);
        InMemoryUserDetailsManager manager = (InMemoryUserDetailsManager) config.userDetailsService(encoder);

        assertThat(manager.loadUserByUsername("admin")).isNotNull();
        assertThat(manager.loadUserByUsername("admin").getPassword()).startsWith("$2a$"); // BCrypt hash
    }

    @Test
    @DisplayName("userDetailsService: throws when ADMIN_PASSWORD is not set")
    void userDetailsService_withoutPassword_throws() {
        Environment env = mock(Environment.class);
        when(env.getProperty("ADMIN_PASSWORD", System.getenv("ADMIN_PASSWORD"))).thenReturn(null);

        SecurityConfig config = new SecurityConfig(env, rateLimitFilter);

        assertThatThrownBy(() -> config.userDetailsService(encoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("passwordEncoder: returns BCryptPasswordEncoder")
    void passwordEncoder_returnsBCrypt() {
        Environment env = mock(Environment.class);
        SecurityConfig config = new SecurityConfig(env, rateLimitFilter);

        PasswordEncoder result = config.passwordEncoder();

        assertThat(result).isInstanceOf(BCryptPasswordEncoder.class);
    }
}
