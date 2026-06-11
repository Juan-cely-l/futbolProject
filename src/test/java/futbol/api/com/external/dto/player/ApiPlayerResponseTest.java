package futbol.api.com.external.dto.player;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiPlayerResponse Record Tests")
class ApiPlayerResponseTest {

    @Test
    @DisplayName("ApiPlayerResponse: constructor and accessors")
    void apiPlayerResponse() {
        var resp = new ApiPlayerResponse(List.of(), new ApiPlayerResponse.Paging(1, 10));
        assertThat(resp.response()).isEmpty();
        assertThat(resp.paging().current()).isEqualTo(1);
        assertThat(resp.paging().total()).isEqualTo(10);
    }

    @Test
    @DisplayName("PlayerEntry: constructor and accessors")
    void playerEntry() {
        var info = new PlayerInfo(1, "Messi", 37, "Forward", "url", 30, 10, 25);
        var stat = new PlayerStat(
                new PlayerStat.Games(25, "Forward"),
                new PlayerStat.Goals(15, 8));
        var entry = new PlayerEntry(info, List.of(stat));

        assertThat(entry.player().name()).isEqualTo("Messi");
        assertThat(entry.player().age()).isEqualTo(37);
        assertThat(entry.player().position()).isEqualTo("Forward");
        assertThat(entry.player().photo()).isEqualTo("url");
        assertThat(entry.player().goals()).isEqualTo(30);
        assertThat(entry.player().assists()).isEqualTo(10);
        assertThat(entry.player().appearences()).isEqualTo(25);
        assertThat(entry.statistics()).hasSize(1);
    }

    @Test
    @DisplayName("PlayerStat: nested record accessors")
    void playerStat() {
        var games = new PlayerStat.Games(30, "Midfielder");
        var goals = new PlayerStat.Goals(10, 5);
        var stat = new PlayerStat(games, goals);

        assertThat(stat.games().appearences()).isEqualTo(30);
        assertThat(stat.games().position()).isEqualTo("Midfielder");
        assertThat(stat.goals().total()).isEqualTo(10);
        assertThat(stat.goals().assists()).isEqualTo(5);
    }
}
