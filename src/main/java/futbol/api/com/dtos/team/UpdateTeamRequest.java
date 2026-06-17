package futbol.api.com.dtos.team;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamRequest {
    @Size(min = 2, max = 100)
    @Pattern(regexp = ".*\\S.*", message = "Team name must not be blank")
    private String name;

    @Min(0)
    private Long budget;

    @Size(min = 2, max = 100)
    @Pattern(regexp = ".*\\S.*", message = "City name must not be blank")
    private String city;
}
