package futbol.api.com.external.mapper;

import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.external.mapper.FootballDataMapper.PlayerData;
import futbol.api.com.external.mapper.FootballDataMapper.TeamInfo;
import futbol.api.com.models.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FootballDataMapper Unit Tests")
class FootballDataMapperTest {

    @Mock
    private MarketValueCalculator marketValueCalculator;

    @InjectMocks
    private FootballDataMapper mapper;

    // -----------------------------------------------------------------------
    // toTeamInfo
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toTeamInfo: normalizes name to lowercase and trims whitespace")
    void toTeamInfo_normalizesName() {
        TeamData data = new TeamData(1, "  FC Barcelona  ", "Spain");

        TeamInfo result = mapper.toTeamInfo(data);

        assertThat(result.name()).isEqualTo("fc barcelona");
        assertThat(result.country()).isEqualTo("Spain");
    }

    @Test
    @DisplayName("toTeamInfo: preserves country as-is")
    void toTeamInfo_preservesCountry() {
        TeamData data = new TeamData(2, "Real Madrid", "Spain");

        TeamInfo result = mapper.toTeamInfo(data);

        assertThat(result.country()).isEqualTo("Spain");
    }

    @Test
    @DisplayName("toTeamInfo: handles already lowercase name with no whitespace")
    void toTeamInfo_alreadyNormalized_returnsSame() {
        TeamData data = new TeamData(3, "juventus", "Italy");

        TeamInfo result = mapper.toTeamInfo(data);

        assertThat(result.name()).isEqualTo("juventus");
    }

    @Test
    @DisplayName("toTeamInfo: handles uppercase name with no whitespace")
    void toTeamInfo_uppercaseName_lowercases() {
        TeamData data = new TeamData(4, "JUVENTUS", "Italy");

        TeamInfo result = mapper.toTeamInfo(data);

        assertThat(result.name()).isEqualTo("juventus");
    }

    // -----------------------------------------------------------------------
    // toPosition
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toPosition: maps Goalkeeper to GOALKEEPER")
    void toPosition_goalkeeper_mapsToGoalkeeper() {
        PlayerInfo player = new PlayerInfo(1, "Gigi", 25, "Goalkeeper", "url", 0, 0, 10);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.GOALKEEPER);
    }

    @Test
    @DisplayName("toPosition: maps Defender to DEFENDER")
    void toPosition_defender_mapsToDefender() {
        PlayerInfo player = new PlayerInfo(2, "Sergio", 30, "Defender", "url", 5, 2, 25);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.DEFENDER);
    }

    @Test
    @DisplayName("toPosition: maps Midfielder to MIDFIELDER")
    void toPosition_midfielder_mapsToMidfielder() {
        PlayerInfo player = new PlayerInfo(3, "Luka", 35, "Midfielder", "url", 8, 12, 30);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.MIDFIELDER);
    }

    @Test
    @DisplayName("toPosition: maps Attacker to FORWARD")
    void toPosition_attacker_mapsToForward() {
        PlayerInfo player = new PlayerInfo(4, "Cristiano", 38, "Attacker", "url", 30, 5, 35);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.FORWARD);
    }

    @Test
    @DisplayName("toPosition: unknown position falls back to MIDFIELDER")
    void toPosition_unknownPosition_fallsBackToMidfielder() {
        PlayerInfo player = new PlayerInfo(5, "Unknown", 22, "Winger", "url", 10, 5, 20);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.MIDFIELDER);
    }

    @Test
    @DisplayName("toPosition: normalizes case so 'goalkeeper' matches GOALKEEPER")
    void toPosition_normalizesCase() {
        PlayerInfo player = new PlayerInfo(7, "Test", 25, "goalkeeper", "url", 0, 0, 10);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.GOALKEEPER);
    }

    // -----------------------------------------------------------------------
    // toPlayerData
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toPlayerData: aggregates all player data correctly")
    void toPlayerData_aggregatesCorrectly() {
        PlayerInfo player = new PlayerInfo(10, "Erling", 22, "Attacker", "photo_url", 28, 3, 30);
        when(marketValueCalculator.calculate(Position.FORWARD, 22, 28, 3, 30)).thenReturn(80_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.position()).isEqualTo(Position.FORWARD);
        assertThat(result.age()).isEqualTo(22);
        assertThat(result.valueMarket()).isEqualTo(80_000_000);
        assertThat(result.photo()).isEqualTo("photo_url");
    }

    @Test
    @DisplayName("toPlayerData: handles null photo gracefully")
    void toPlayerData_nullPhoto_preservesNull() {
        PlayerInfo player = new PlayerInfo(11, "NoPhoto", 25, "Defender", null, 5, 3, 20);
        when(marketValueCalculator.calculate(Position.DEFENDER, 25, 5, 3, 20)).thenReturn(15_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.photo()).isNull();
        assertThat(result.position()).isEqualTo(Position.DEFENDER);
        assertThat(result.age()).isEqualTo(25);
    }

    // -----------------------------------------------------------------------
    // toPosition edge cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toPosition: null position returns MIDFIELDER")
    void toPosition_nullPosition_returnsMidfielder() {
        PlayerInfo player = new PlayerInfo(20, "NullPos", 25, null, "url", 0, 0, 10);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.MIDFIELDER);
    }

    @Test
    @DisplayName("toPosition: blank string position returns MIDFIELDER")
    void toPosition_blankString_returnsMidfielder() {
        PlayerInfo player = new PlayerInfo(21, "BlankPos", 25, "", "url", 0, 0, 10);

        Position result = mapper.toPosition(player);

        assertThat(result).isEqualTo(Position.MIDFIELDER);
    }

    // -----------------------------------------------------------------------
    // toPlayerData null field defaults
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toPlayerData: null age defaults to 25")
    void toPlayerData_nullAge_defaultsTo25() {
        PlayerInfo player = new PlayerInfo(30, "NoAge", null, "Defender", "url", 5, 3, 20);
        when(marketValueCalculator.calculate(Position.DEFENDER, 25, 5, 3, 20)).thenReturn(10_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.age()).isEqualTo(25);
        assertThat(result.position()).isEqualTo(Position.DEFENDER);
        assertThat(result.valueMarket()).isEqualTo(10_000_000);
    }

    @Test
    @DisplayName("toPlayerData: null goals defaults to 0")
    void toPlayerData_nullGoals_defaultsTo0() {
        PlayerInfo player = new PlayerInfo(31, "NoGoals", 25, "Attacker", "url", null, 3, 20);
        when(marketValueCalculator.calculate(Position.FORWARD, 25, 0, 3, 20)).thenReturn(15_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.age()).isEqualTo(25);
        assertThat(result.position()).isEqualTo(Position.FORWARD);
        assertThat(result.valueMarket()).isEqualTo(15_000_000);
    }

    @Test
    @DisplayName("toPlayerData: null assists defaults to 0")
    void toPlayerData_nullAssists_defaultsTo0() {
        PlayerInfo player = new PlayerInfo(32, "NoAssists", 25, "Midfielder", "url", 5, null, 20);
        when(marketValueCalculator.calculate(Position.MIDFIELDER, 25, 5, 0, 20)).thenReturn(12_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.age()).isEqualTo(25);
        assertThat(result.position()).isEqualTo(Position.MIDFIELDER);
        assertThat(result.valueMarket()).isEqualTo(12_000_000);
    }

    @Test
    @DisplayName("toPlayerData: null appearences defaults to 0")
    void toPlayerData_nullAppearences_defaultsTo0() {
        PlayerInfo player = new PlayerInfo(33, "NoMatches", 25, "Goalkeeper", "url", 0, 0, null);
        when(marketValueCalculator.calculate(Position.GOALKEEPER, 25, 0, 0, 0)).thenReturn(8_000_000);

        PlayerData result = mapper.toPlayerData(player);

        assertThat(result.age()).isEqualTo(25);
        assertThat(result.position()).isEqualTo(Position.GOALKEEPER);
        assertThat(result.valueMarket()).isEqualTo(8_000_000);
        assertThat(result.photo()).isEqualTo("url");
    }
}
