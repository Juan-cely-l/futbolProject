package futbol.api.com.external.dto;

import java.util.List;

public record SyncTeamResult(
        String name,
        String country,
        boolean created,
        boolean updated,
        List<SyncPlayerResult> players
) {
}
