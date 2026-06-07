package futbol.api.com.dtos.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamValueResponseTest {

    @Test
    @DisplayName("Builder creates object with correct values")
    void builder_createsObjectWithCorrectValues() {
        TeamValueResponse response = TeamValueResponse.builder()
                .teamName("FC Barcelona")
                .totalValue(500_000_000L)
                .build();

        assertThat(response.getTeamName()).isEqualTo("FC Barcelona");
        assertThat(response.getTotalValue()).isEqualTo(500_000_000L);
    }

    @Test
    @DisplayName("Getters return correct values")
    void getters_returnCorrectValues() {
        TeamValueResponse response = TeamValueResponse.builder()
                .teamName("Real Madrid")
                .totalValue(750_000_000L)
                .build();

        assertThat(response.getTeamName()).isEqualTo("Real Madrid");
        assertThat(response.getTotalValue()).isEqualTo(750_000_000L);
    }

    @Test
    @DisplayName("Null fields are allowed")
    void nullFields_areAllowed() {
        TeamValueResponse response = TeamValueResponse.builder()
                .teamName(null)
                .totalValue(null)
                .build();

        assertThat(response.getTeamName()).isNull();
        assertThat(response.getTotalValue()).isNull();
    }
}
