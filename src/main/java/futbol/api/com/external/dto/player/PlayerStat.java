package futbol.api.com.external.dto.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerStat(
        Games games,
        Goals goals
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Games(Integer appearences, String position) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer total, Integer assists) {}
}
