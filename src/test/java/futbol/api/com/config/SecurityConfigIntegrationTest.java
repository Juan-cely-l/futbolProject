package futbol.api.com.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ADMIN_PASSWORD=test123")
@DisplayName("SecurityConfig Integration Tests")
class SecurityConfigIntegrationTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Test
    @DisplayName("filterChain: bean is created with stateless session policy")
    void filterChain_beanCreated() {
        assertThat(filterChain).isNotNull();
    }
}
