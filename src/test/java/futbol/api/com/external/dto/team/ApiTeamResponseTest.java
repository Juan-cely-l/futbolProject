package futbol.api.com.external.dto.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiTeamResponse Record Tests")
class ApiTeamResponseTest {

    @Test
    @DisplayName("ApiTeamResponse: constructor and accessors")
    void apiTeamResponse() {
        var teamData = new TeamData(1, "FC Barcelona", "Spain");
        var entry = new TeamEntry(teamData);
        var resp = new ApiTeamResponse(List.of(entry));

        assertThat(resp.response()).hasSize(1);
        assertThat(resp.response().get(0).team().id()).isEqualTo(1);
        assertThat(resp.response().get(0).team().name()).isEqualTo("FC Barcelona");
        assertThat(resp.response().get(0).team().country()).isEqualTo("Spain");
    }
}
