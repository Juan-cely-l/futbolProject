package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdatePlayerRequest {
    @Size(min=2,max=50)
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

    private String teamName;

    @Size(max = 500)
    private String photo;}
