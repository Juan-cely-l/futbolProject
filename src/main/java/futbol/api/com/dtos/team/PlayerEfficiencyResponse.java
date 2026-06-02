package futbol.api.com.dtos.team;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;
@Getter
@Builder
public class PlayerEfficiencyResponse {
    private UUID playerId;
    private String playerName;
    private double contributionsPerMatch;
}
