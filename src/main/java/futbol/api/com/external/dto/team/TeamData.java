package futbol.api.com.external.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamData(
        Integer id,
        String name,
        String country
) {
}
