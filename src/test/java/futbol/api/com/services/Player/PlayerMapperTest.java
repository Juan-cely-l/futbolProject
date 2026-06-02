package futbol.api.com.services.Player;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlayerMapper Unit Tests")
class PlayerMapperTest {

    private final PlayerMapper playerMapper = new PlayerMapper();

    @Test
    @DisplayName("mapPlayerToResponseDto: maps all fields when team is present")
    void mapPlayerToResponseDto_withTeam_mapsAllFields() {
        Team team = Team.builder()
                .id(UUID.randomUUID()).name("fc barcelona").build();

        UUID playerId = UUID.randomUUID();
        Player player = Player.builder()
                .id(playerId)
                .name("lionel messi")
                .goals(25)
                .position(Position.FORWARD)
                .age(33)
                .assists(10)
                .matches(30)
                .valueMarket(120_000_000)
                .team(team)
                .build();

        PlayerResponse response = playerMapper.mapPlayerToResponseDto(player);

        assertThat(response.getId()).isEqualTo(playerId);
        assertThat(response.getName()).isEqualTo("lionel messi");
        assertThat(response.getGoals()).isEqualTo(25);
        assertThat(response.getPosition()).isEqualTo(Position.FORWARD);
        assertThat(response.getAge()).isEqualTo(33);
        assertThat(response.getAssists()).isEqualTo(10);
        assertThat(response.getMatches()).isEqualTo(30);
        assertThat(response.getValueMarket()).isEqualTo(120_000_000);
        assertThat(response.getTeamName()).isEqualTo("fc barcelona");
    }

    @Test
    @DisplayName("mapPlayerToResponseDto: sets teamName from team")
    void mapPlayerToResponseDto_withTeam_teamNameIsMapped() {
        Team team = Team.builder()
                .id(UUID.randomUUID()).name("fc barcelona").build();
        Player player = Player.builder()
                .id(UUID.randomUUID())
                .name("lionel messi")
                .goals(25)
                .position(Position.FORWARD)
                .age(33)
                .assists(10)
                .matches(30)
                .valueMarket(120_000_000)
                .team(team)
                .build();

        PlayerResponse response = playerMapper.mapPlayerToResponseDto(player);

        assertThat(response.getTeamName()).isEqualTo("fc barcelona");
    }

    @Test
    @DisplayName("mapPlayerToResponseDto: maps null numeric fields correctly")
    void mapPlayerToResponseDto_withNullNumerics_mapsCorrectly() {
        Team team = Team.builder()
                .id(UUID.randomUUID()).name("team").build();
        Player player = Player.builder()
                .id(UUID.randomUUID())
                .name("incomplete player")
                .goals(null)
                .position(Position.DEFENDER)
                .age(null)
                .assists(null)
                .matches(null)
                .valueMarket(null)
                .team(team)
                .build();

        PlayerResponse response = playerMapper.mapPlayerToResponseDto(player);

        assertThat(response.getName()).isEqualTo("incomplete player");
        assertThat(response.getPosition()).isEqualTo(Position.DEFENDER);
        assertThat(response.getGoals()).isNull();
        assertThat(response.getAge()).isNull();
        assertThat(response.getAssists()).isNull();
        assertThat(response.getMatches()).isNull();
        assertThat(response.getValueMarket()).isNull();
        assertThat(response.getTeamName()).isEqualTo("team");
    }
}
