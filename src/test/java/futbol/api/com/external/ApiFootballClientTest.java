package futbol.api.com.external;

import futbol.api.com.exceptions.ExternalApiException;
import futbol.api.com.external.client.RequestCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiFootballClient Unit Tests")
class ApiFootballClientTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RequestCounter requestCounter;
    @Mock
    private RestClient.RequestHeadersUriSpec uriSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ApiFootballClient client;

    @BeforeEach
    void setUp() {
        client = new ApiFootballClient(restClient, requestCounter);
    }

    @Test
    @DisplayName("getTeamsByLeague: propagates ExternalApiException from RestClient")
    void getTeamsByLeague_error_throws() {
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class)))
                .thenThrow(new ExternalApiException(403, "Forbidden"));

        assertThatThrownBy(() -> client.getTeamsByLeague(1, 2025))
                .isInstanceOf(ExternalApiException.class);

        verify(requestCounter).increment();
    }

    @Test
    @DisplayName("getPlayersByTeam: propagates ExternalApiException from RestClient")
    void getPlayersByTeam_error_throws() {
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class)))
                .thenThrow(new ExternalApiException(403, "Forbidden"));

        assertThatThrownBy(() -> client.getPlayersByTeam(1, 2025, 1))
                .isInstanceOf(ExternalApiException.class);

        verify(requestCounter).increment();
    }
}
