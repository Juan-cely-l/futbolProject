package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerResponseTest {

    @Test
    @DisplayName("Should build response with all fields")
    void buildWithAllFields() {
        UUID id = UUID.randomUUID();
        PlayerResponse response = PlayerResponse.builder()
                .id(id)
                .name("Lionel Messi")
                .goals(30)
                .position(Position.FORWARD)
                .age(25)
                .assists(15)
                .matches(40)
                .valueMarket(100_000_000)
                .teamName("FC Barcelona")
                .build();

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Lionel Messi");
        assertThat(response.getGoals()).isEqualTo(30);
        assertThat(response.getPosition()).isEqualTo(Position.FORWARD);
        assertThat(response.getAge()).isEqualTo(25);
        assertThat(response.getAssists()).isEqualTo(15);
        assertThat(response.getMatches()).isEqualTo(40);
        assertThat(response.getValueMarket()).isEqualTo(100_000_000);
        assertThat(response.getTeamName()).isEqualTo("FC Barcelona");
    }

    @Test
    @DisplayName("Should build response with null teamName")
    void buildWithNullTeamName() {
        PlayerResponse response = PlayerResponse.builder()
                .id(UUID.randomUUID())
                .name("Player")
                .goals(0)
                .position(Position.DEFENDER)
                .age(20)
                .assists(0)
                .matches(0)
                .valueMarket(0)
                .teamName(null)
                .build();

        assertThat(response.getTeamName()).isNull();
    }

    @Test
    @DisplayName("Should build response with GOALKEEPER position")
    void buildWithGoalkeeper() {
        PlayerResponse response = PlayerResponse.builder()
                .id(UUID.randomUUID())
                .name("Courtois")
                .goals(0)
                .position(Position.GOALKEEPER)
                .age(30)
                .assists(0)
                .matches(50)
                .valueMarket(50_000_000)
                .teamName("Real Madrid")
                .build();

        assertThat(response.getPosition()).isEqualTo(Position.GOALKEEPER);
    }
}
