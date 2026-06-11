package futbol.api.com.external.dto;

import java.util.List;

public record SyncRequest(
        List<Integer> leagueIds,
        Integer season,
        Integer maxTeams
) {}