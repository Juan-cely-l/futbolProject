package futbol.api.com.external.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiTeamResponse(
        List<TeamEntry> response
) {
}
