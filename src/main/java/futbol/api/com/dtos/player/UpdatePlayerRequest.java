package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePlayerRequest {
    @Size(min = 2, max = 50)
    @Pattern(regexp = ".*\\S.*", message = "Player name must not be blank")
    private String name;

    @Min(0)
    private Integer goals;

    private Position position;

    @Min(15)
    private Integer age;

    @Min(0)
    private Integer assists;

    @Min(0)
    private Integer matches;

    @Min(0)
    private Integer valueMarket;

    @Size(max = 100, message = "Team name must be at most 100 characters")
    @Pattern(regexp = ".*\\S.*", message = "Team name must not be blank")
    private String teamName;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://).*", message = "Photo URL must start with http:// or https://")
    private String photo;
}
