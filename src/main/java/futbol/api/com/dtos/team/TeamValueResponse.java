package futbol.api.com.dtos.team;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TeamValueResponse {
    private String teamName;
    private Long totalValue;
}
