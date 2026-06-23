package futbol.api.com.external.service;

import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import futbol.api.com.external.dto.team.TeamData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueProcessor Unit Tests")
class LeagueProcessorTest {

    @Mock
    private FootballApiProvider apiClient;
    @Mock
    private RequestCounter requestCounter;
    @Mock
    private TeamProcessor teamProcessor;

    private LeagueProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LeagueProcessor(apiClient, requestCounter, teamProcessor, () -> {});
    }

    @Test
    @DisplayName("processLeague: maxTeams smaller than team count limits estimate and processed teams")
    void processLeague_maxTeamsSmallerThanTeamCount_limitsEstimateAndProcessedTeams() {
        TeamData arsenal = new TeamData(1, "Arsenal", "England");
        TeamData chelsea = new TeamData(2, "Chelsea", "England");
        TeamData liverpool = new TeamData(3, "Liverpool", "England");
        when(apiClient.getTeamsByLeague(39, 2025)).thenReturn(List.of(arsenal, chelsea, liverpool));
        when(requestCounter.remaining()).thenReturn(100);
        when(teamProcessor.processTeam(arsenal, 39, 2025)).thenReturn(TeamProcessingResult.empty("arsenal"));
        when(teamProcessor.processTeam(chelsea, 39, 2025)).thenReturn(TeamProcessingResult.empty("chelsea"));

        LeagueProcessingResult result = processor.processLeague(39, 2025, 2);

        assertThat(result.estimatedRequests()).isEqualTo(11);
        assertThat(result.totalTeams()).isEqualTo(2);
        assertThat(result.processedTeams()).isEqualTo(2);
        verify(teamProcessor).processTeam(arsenal, 39, 2025);
        verify(teamProcessor).processTeam(chelsea, 39, 2025);
        verify(teamProcessor, never()).processTeam(liverpool, 39, 2025);
    }

    @Test
    @DisplayName("processLeague: null maxTeams processes all provider teams")
    void processLeague_nullMaxTeams_processesAllProviderTeams() {
        TeamData arsenal = new TeamData(1, "Arsenal", "England");
        TeamData chelsea = new TeamData(2, "Chelsea", "England");
        when(apiClient.getTeamsByLeague(39, 2025)).thenReturn(List.of(arsenal, chelsea));
        when(requestCounter.remaining()).thenReturn(100);
        when(teamProcessor.processTeam(arsenal, 39, 2025)).thenReturn(TeamProcessingResult.empty("arsenal"));
        when(teamProcessor.processTeam(chelsea, 39, 2025)).thenReturn(TeamProcessingResult.empty("chelsea"));

        LeagueProcessingResult result = processor.processLeague(39, 2025, null);

        assertThat(result.estimatedRequests()).isEqualTo(11);
        assertThat(result.totalTeams()).isEqualTo(2);
        assertThat(result.processedTeams()).isEqualTo(2);
        verify(teamProcessor, times(2)).processTeam(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(39), org.mockito.ArgumentMatchers.eq(2025));
    }

    @Test
    @DisplayName("processLeague: estimate fallback fetches fresh teams and applies maxTeams")
    void processLeague_estimateFallbackFetchesFreshTeams_appliesMaxTeams() {
        TeamData arsenal = new TeamData(1, "Arsenal", "England");
        TeamData chelsea = new TeamData(2, "Chelsea", "England");
        when(apiClient.getTeamsByLeague(39, 2025))
                .thenThrow(new RuntimeException("temporary estimate failure"))
                .thenReturn(List.of(arsenal, chelsea));
        when(requestCounter.remaining()).thenReturn(100);
        when(teamProcessor.processTeam(arsenal, 39, 2025)).thenReturn(TeamProcessingResult.empty("arsenal"));

        LeagueProcessingResult result = processor.processLeague(39, 2025, 1);

        assertThat(result.estimatedRequests()).isEqualTo(20);
        assertThat(result.totalTeams()).isEqualTo(1);
        assertThat(result.processedTeams()).isEqualTo(1);
        verify(teamProcessor).processTeam(arsenal, 39, 2025);
        verify(teamProcessor, never()).processTeam(chelsea, 39, 2025);
    }

    @Test
    @DisplayName("processLeague: estimated requests exceeding remaining skips league")
    void processLeague_estimatedRequestsExceedRemaining_skipsLeagueAndRecordsError() {
        when(apiClient.getTeamsByLeague(39, 2025)).thenReturn(List.of(
                new TeamData(1, "Arsenal", "England"),
                new TeamData(2, "Chelsea", "England")
        ));
        when(requestCounter.remaining()).thenReturn(3);

        LeagueProcessingResult result = processor.processLeague(39, 2025, null);

        assertThat(result.skipped()).isTrue();
        assertThat(result.processedTeams()).isZero();
        assertThat(result.errors()).singleElement().asString().contains("League 39").contains("only 3 requests remaining");
        verify(teamProcessor, never()).processTeam(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
