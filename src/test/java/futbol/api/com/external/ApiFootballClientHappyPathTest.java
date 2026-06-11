package futbol.api.com.external;

import futbol.api.com.exceptions.ExternalApiException;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.dto.player.ApiPlayerResponse;
import futbol.api.com.external.dto.player.PlayerEntry;
import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.player.PlayerStat;
import futbol.api.com.external.dto.team.ApiTeamResponse;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.dto.team.TeamEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiFootballClient Happy Path Tests")
class ApiFootballClientHappyPathTest {

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
    @DisplayName("getTeamsByLeague: returns mapped team data on success")
    void getTeamsByLeague_success() {
        var teamData = new TeamData(1, "FC Barcelona", "Spain");
        var apiResponse = new ApiTeamResponse(List.of(new TeamEntry(teamData)));

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(apiResponse);

        List<TeamData> result = client.getTeamsByLeague(1, 2025);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("FC Barcelona");
        assertThat(result.get(0).country()).isEqualTo("Spain");
        verify(requestCounter).increment();
    }

    @Test
    @DisplayName("getPlayersByTeam: returns mapped player info with stats")
    void getPlayersByTeam_success_withStats() {
        var games = new PlayerStat.Games(25, "Forward");
        var goals = new PlayerStat.Goals(15, 8);
        var stat = new PlayerStat(games, goals);
        var playerInfo = new PlayerInfo(1, "Messi", 37, "Forward", "url", 30, 10, 25);
        var entry = new PlayerEntry(playerInfo, List.of(stat));
        var paging = new ApiPlayerResponse.Paging(1, 1);
        var apiResponse = new ApiPlayerResponse(List.of(entry), paging);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(apiResponse);

        List<PlayerInfo> result = client.getPlayersByTeam(1, 2025, 140);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Messi");
        assertThat(result.get(0).age()).isEqualTo(37);
        // Values mapped from PlayerStat (not raw PlayerInfo)
        assertThat(result.get(0).position()).isEqualTo("Forward");
        assertThat(result.get(0).goals()).isEqualTo(15);     // from stats.goals().total()
        assertThat(result.get(0).assists()).isEqualTo(8);    // from stats.goals().assists()
        assertThat(result.get(0).appearences()).isEqualTo(25); // from stats.games().appearences()
        verify(requestCounter, times(1)).increment();
    }

    @Test
    @DisplayName("getPlayersByTeam: handles null statistics gracefully")
    void getPlayersByTeam_nullStats() {
        var playerInfo = new PlayerInfo(1, "Saka", 23, null, "url", null, null, null);
        var entry = new PlayerEntry(playerInfo, null);
        var paging = new ApiPlayerResponse.Paging(1, 1);
        var apiResponse = new ApiPlayerResponse(List.of(entry), paging);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(apiResponse);

        List<PlayerInfo> result = client.getPlayersByTeam(1, 2025, 140);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Saka");
        assertThat(result.get(0).age()).isEqualTo(23);
        // Null stats → defaults to 0
        assertThat(result.get(0).goals()).isZero();
        assertThat(result.get(0).assists()).isZero();
        assertThat(result.get(0).appearences()).isZero();
    }
}
