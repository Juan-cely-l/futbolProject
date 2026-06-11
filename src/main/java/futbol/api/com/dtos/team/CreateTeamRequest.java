package futbol.api.com.dtos.team;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CreateTeamRequest {
    @NotBlank(message = "The team should have a name")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;
    @NotNull
    @Min(0)
    private Long budget;
    @NotBlank(message = "The team should have a city")
    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    private String city;
}
