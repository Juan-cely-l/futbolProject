package futbol.api.com.dtos.team;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TeamResponse {
    private UUID id;
    private String name;
    private Long budget;
    private String city;
    private int squadCount;
    private LocalDateTime createdAt;
}
