package futbol.api.com.dtos.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamResponseTest {

    @Test
    @DisplayName("Should build response with all fields")
    void buildWithAllFields() {
        UUID id = UUID.randomUUID();
        TeamResponse response = TeamResponse.builder()
                .id(id)
                .name("Real Madrid")
                .budget(500_000_000L)
                .city("Madrid")
                .build();

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("Real Madrid");
        assertThat(response.getBudget()).isEqualTo(500_000_000L);
        assertThat(response.getCity()).isEqualTo("Madrid");
    }

    @Test
    @DisplayName("Should build response with null budget")
    void buildWithNullBudget() {
        TeamResponse response = TeamResponse.builder()
                .id(UUID.randomUUID())
                .name("Team")
                .budget(null)
                .city("City")
                .build();

        assertThat(response.getBudget()).isNull();
        assertThat(response.getName()).isEqualTo("Team");
    }
}
