package futbol.api.com.external.dto.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiPlayerResponse(
        List<PlayerEntry> response,
        Paging paging
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paging(Integer current, Integer total) {}
}
