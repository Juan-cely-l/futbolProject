package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePlayerRequest {

    @NotBlank(message = "The Player should have a name")
    private String name;

    @NotNull(message = "The goals cant be null")
    @Min(value = 0,message = "The player should have at least 0 goals")
    private Integer goals;

    @NotNull
    private Position position;

    @NotNull(message = "The age cant be null")
    @Min(value = 15, message = "The minimum age is 15")

    private Integer age;

    @NotNull(message = "The assists cant be null")
    @Min(value = 0,message = "The player should have at least 0 assits")
    private Integer assists;

    @NotNull(message = "The matches cant be null")
    @Min(value = 0,message = "The player should have at least 0 matches")
    private Integer matches;

    @NotNull(message = "The valueMarket cant be null")
    @Min(value = 0,message = "The player should have at least 0 Value Market")
    private Integer valueMarket;

    @NotBlank
    private String teamName;

}
