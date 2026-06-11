package futbol.api.com;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("FutbolApplication Smoke Test")
class FutbolApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("application context loads successfully")
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}
