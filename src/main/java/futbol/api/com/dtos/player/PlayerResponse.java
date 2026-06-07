package futbol.api.com.dtos.player;

import futbol.api.com.models.Position;
import lombok.Builder;
import lombok.Getter;


import java.util.UUID;
@Getter
@Builder
public class PlayerResponse {
    private UUID id;//Tipo de datos usado en ID

    private String name;

    private Integer goals;

    private Position position;

    private Integer age;

    private Integer assists;

    private Integer matches;

    private Integer valueMarket;

    private String teamName;

    private String photo;
}
