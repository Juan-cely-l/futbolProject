package futbol.api.com.external.dto.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerEntry(
        PlayerInfo player,
        List<PlayerStat> statistics
) {
}
