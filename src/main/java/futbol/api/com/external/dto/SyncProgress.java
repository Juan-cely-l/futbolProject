package futbol.api.com.external.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SyncProgress(
        Status status,
        List<Integer> leagueIds,
        int totalLeagues,
        int processedLeagues,
        int totalTeams,
        int processedTeams,
        int playersCreated,
        int playersUpdated,
        Integer season,
        List<String> errors,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<SyncTeamResult> teams
) {
}
