package futbol.api.com.external.service;

import futbol.api.com.external.dto.SyncTeamResult;

import java.util.List;

record LeagueProcessingResult(
        int estimatedRequests,
        int totalTeams,
        int processedTeams,
        int playersCreated,
        int playersUpdated,
        boolean leagueProcessed,
        boolean skipped,
        List<String> errors,
        List<SyncTeamResult> teamResults
) {

    static LeagueProcessingResult success(
            int totalTeams,
            int processedTeams,
            int playersCreated,
            int playersUpdated,
            List<SyncTeamResult> teamResults
    ) {
        return new LeagueProcessingResult(
                0,
                totalTeams,
                processedTeams,
                playersCreated,
                playersUpdated,
                true,
                false,
                List.of(),
                teamResults
        );
    }

    static LeagueProcessingResult skipped(int estimatedRequests, String message) {
        return new LeagueProcessingResult(
                estimatedRequests,
                0,
                0,
                0,
                0,
                false,
                true,
                List.of(message),
                List.of()
        );
    }
}
