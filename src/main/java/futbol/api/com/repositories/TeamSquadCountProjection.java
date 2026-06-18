package futbol.api.com.repositories;

import java.util.UUID;

public interface TeamSquadCountProjection {
    UUID getTeamId();

    long getSquadCount();
}
