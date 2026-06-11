package futbol.api.com.external.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sync Result Record Tests")
class SyncResultTest {

    @Test
    @DisplayName("SyncPlayerResult: constructor and accessors")
    void syncPlayerResult() {
        var result = new SyncPlayerResult("Messi", "Forward", 37, "photo.jpg", 30, 10, 25, 50_000_000);

        assertThat(result.name()).isEqualTo("Messi");
        assertThat(result.position()).isEqualTo("Forward");
        assertThat(result.age()).isEqualTo(37);
        assertThat(result.photo()).isEqualTo("photo.jpg");
        assertThat(result.goals()).isEqualTo(30);
        assertThat(result.assists()).isEqualTo(10);
        assertThat(result.matches()).isEqualTo(25);
        assertThat(result.valueMarket()).isEqualTo(50_000_000);
    }

    @Test
    @DisplayName("SyncTeamResult: constructor and accessors")
    void syncTeamResult() {
        var player = new SyncPlayerResult("Messi", "Forward", 37, "photo.jpg", 30, 10, 25, 50_000_000);
        var result = new SyncTeamResult("FC Barcelona", "Spain", true, false, List.of(player));

        assertThat(result.name()).isEqualTo("FC Barcelona");
        assertThat(result.country()).isEqualTo("Spain");
        assertThat(result.created()).isTrue();
        assertThat(result.updated()).isFalse();
        assertThat(result.players()).hasSize(1);
    }
}
