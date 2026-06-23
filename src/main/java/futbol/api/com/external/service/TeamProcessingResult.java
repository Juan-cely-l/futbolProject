package futbol.api.com.external.service;

import futbol.api.com.external.dto.SyncTeamResult;

import java.util.List;

record TeamProcessingResult(
        int playersCreated,
        int playersUpdated,
        SyncTeamResult teamResult,
        List<String> errors
) {

    static TeamProcessingResult empty(String teamName) {
        return new TeamProcessingResult(
                0,
                0,
                new SyncTeamResult(teamName, null, false, true, List.of()),
                List.of()
        );
    }
}
