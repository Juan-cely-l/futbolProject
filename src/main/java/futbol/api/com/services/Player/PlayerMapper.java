package futbol.api.com.services.Player;

import futbol.api.com.dtos.player.PlayerResponse;
import futbol.api.com.models.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {
    public PlayerResponse mapPlayerToResponseDto(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .name(player.getName())
                .goals(player.getGoals())
                .position(player.getPosition())
                .age(player.getAge())
                .assists(player.getAssists())
                .matches(player.getMatches())
                .valueMarket(player.getValueMarket())
                .teamName(player.getTeam() != null ? player.getTeam().getName() : null)
                .photo(player.getPhoto())
                .build();
    }
}
