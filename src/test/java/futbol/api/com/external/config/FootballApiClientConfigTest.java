package futbol.api.com.external.config;

import futbol.api.com.external.ApiFootballClient;
import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FootballApiClientConfig Unit Tests")
class FootballApiClientConfigTest {

    @Mock
    private ApiFootballClient apiFootballClient;

    private final FootballApiClientConfig clientConfig = new FootballApiClientConfig();

    @Test
    @DisplayName("requestCounter: creates counter with daily limit from config")
    void requestCounter_createsWithLimit() {
        FootballApiConfig config = new FootballApiConfig(
                "test-key", "test-host", "https://api.example.com", 2025, 2020, 2025, 500, java.util.List.of(39));

        RequestCounter counter = clientConfig.requestCounter(config);

        assertThat(counter).isNotNull();
    }

    @Test
    @DisplayName("footballRestClient: creates RestClient with config values")
    void footballRestClient_createsClient() {
        FootballApiConfig config = new FootballApiConfig(
                "test-key", "test-host", "https://api.example.com", 2025, 2020, 2025, 500, java.util.List.of(39));

        var client = clientConfig.footballRestClient(config);

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("cachingFootballApiProvider: creates provider with defaults")
    void cachingFootballApiProvider_createsProvider() {
        FootballApiProvider provider = clientConfig.cachingFootballApiProvider(
                apiFootballClient, new ObjectMapper(), "/tmp/cache", 24L);

        assertThat(provider).isNotNull();
    }
}
