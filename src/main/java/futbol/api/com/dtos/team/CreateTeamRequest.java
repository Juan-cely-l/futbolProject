package futbol.api.com.dtos.team;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CreateTeamRequest {
    @NotBlank(message = "The team should have a name") //Para String que no puedan estar vacias
    private String name;
    @NotNull
    @Min(0)
    private Long budget;
    @NotBlank(message = "The team should have a city")
    private String city;
}
