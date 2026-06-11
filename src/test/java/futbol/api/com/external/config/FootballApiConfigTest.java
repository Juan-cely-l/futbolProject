package futbol.api.com.external.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FootballApiConfig Unit Tests")
class FootballApiConfigTest {

    @Test
    @DisplayName("record: creates config with all fields")
    void record_createsConfig() {
        FootballApiConfig config = new FootballApiConfig(
                "test-key",
                "api.example.com",
                "https://api.example.com",
                2025,
                2020,
                2025,
                100,
                List.of(1, 2, 3)
        );

        assertThat(config.apiKey()).isEqualTo("test-key");
        assertThat(config.apiHost()).isEqualTo("api.example.com");
        assertThat(config.baseUrl()).isEqualTo("https://api.example.com");
        assertThat(config.season()).isEqualTo(2025);
        assertThat(config.seasonMin()).isEqualTo(2020);
        assertThat(config.seasonMax()).isEqualTo(2025);
        assertThat(config.dailyLimit()).isEqualTo(100);
        assertThat(config.leagueIds()).containsExactly(1, 2, 3);
    }
}
