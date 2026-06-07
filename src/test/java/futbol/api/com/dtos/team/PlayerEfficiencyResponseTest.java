package futbol.api.com.dtos.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerEfficiencyResponseTest {

    @Test
    @DisplayName("Builder creates object with correct values")
    void builder_createsObjectWithCorrectValues() {
        UUID playerId = UUID.randomUUID();
        PlayerEfficiencyResponse response = PlayerEfficiencyResponse.builder()
                .playerId(playerId)
                .playerName("Lionel Messi")
                .contributionsPerMatch(1.5)
                .build();

        assertThat(response.getPlayerId()).isEqualTo(playerId);
        assertThat(response.getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(response.getContributionsPerMatch()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Double precision is preserved")
    void doublePrecision_isPreserved() {
        PlayerEfficiencyResponse response = PlayerEfficiencyResponse.builder()
                .playerId(UUID.randomUUID())
                .playerName("Cristiano Ronaldo")
                .contributionsPerMatch(0.3333333333)
                .build();

        assertThat(response.getContributionsPerMatch()).isEqualTo(0.3333333333);
    }

    @Test
    @DisplayName("Zero value works")
    void zeroValue_works() {
        PlayerEfficiencyResponse response = PlayerEfficiencyResponse.builder()
                .playerId(UUID.randomUUID())
                .playerName("Defensive Midfielder")
                .contributionsPerMatch(0.0)
                .build();

        assertThat(response.getContributionsPerMatch()).isEqualTo(0.0);
        assertThat(response.getPlayerName()).isEqualTo("Defensive Midfielder");
    }
}
