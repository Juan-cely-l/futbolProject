package futbol.api.com.external.dto.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerInfo(
        Integer id,
        String name,
        Integer age,
        String position,
        String photo,
        Integer goals,
        Integer assists,
        Integer appearences
) {
}
