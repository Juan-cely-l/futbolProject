package futbol.api.com.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeedController unit tests")
class SeedControllerTest {

    @Mock
    private DataSeeder dataSeeder;

    @InjectMocks
    private SeedController seedController;

    @Test
    @DisplayName("POST /futbix/v1/seed -> 200 OK with seed result")
    void runSeed_returnsOkWithSeedResult() {
        DataSeeder.SeedResult seedResult = new DataSeeder.SeedResult(5, 56, "Seed complete: 5 teams created, 56 players created");
        when(dataSeeder.runSeed()).thenReturn(seedResult);

        ResponseEntity<Map<String, Object>> response = seedController.runSeed();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("complete");
        assertThat(response.getBody().get("teamsCreated")).isEqualTo(5);
        assertThat(response.getBody().get("playersCreated")).isEqualTo(56);
        assertThat(response.getBody().get("message")).isEqualTo("Seed complete: 5 teams created, 56 players created");
    }

    @Test
    @DisplayName("POST /futbix/v1/seed -> DataSeeder.runSeed is called once")
    void runSeed_callsDataSeederOnce() {
        DataSeeder.SeedResult seedResult = new DataSeeder.SeedResult(0, 0, "Seed complete: 0 teams created, 0 players created");
        when(dataSeeder.runSeed()).thenReturn(seedResult);

        seedController.runSeed();

        verify(dataSeeder).runSeed();
    }
}
