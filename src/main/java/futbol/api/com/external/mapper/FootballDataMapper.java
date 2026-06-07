package futbol.api.com.external.mapper;

import futbol.api.com.external.dto.player.PlayerInfo;
import futbol.api.com.external.dto.team.TeamData;
import futbol.api.com.models.Position;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FootballDataMapper {

    private final MarketValueCalculator marketValueCalculator;

    private static final Map<String, Position> POSITION_MAP = Map.of(
            "Goalkeeper", Position.GOALKEEPER,
            "Defender", Position.DEFENDER,
            "Midfielder", Position.MIDFIELDER,
            "Attacker", Position.FORWARD
    );

    public record TeamInfo(String name, String country) {}

    public record PlayerData(Position position, Integer age, Integer valueMarket, String photo) {}

    public FootballDataMapper(MarketValueCalculator marketValueCalculator) {
        this.marketValueCalculator = marketValueCalculator;
    }

    public TeamInfo toTeamInfo(TeamData data) {
        return new TeamInfo(TeamNameNormalizer.normalize(data.name()), data.country());
    }

    public Position toPosition(PlayerInfo player) {
        String pos = player.position();
        if (pos == null || pos.isBlank()) return Position.MIDFIELDER;
        String normalized = pos.substring(0, 1).toUpperCase() + pos.substring(1).toLowerCase();
        return POSITION_MAP.getOrDefault(normalized, Position.MIDFIELDER);
    }

    public PlayerData toPlayerData(PlayerInfo player) {
        Position position = toPosition(player);
        int age = player.age() != null ? player.age() : 25;
        int goals = player.goals() != null ? player.goals() : 0;
        int assists = player.assists() != null ? player.assists() : 0;
        int matches = player.appearences() != null ? player.appearences() : 0;
        int value = marketValueCalculator.calculate(position, age, goals, assists, matches);
        return new PlayerData(position, age, value, player.photo());
    }
}
