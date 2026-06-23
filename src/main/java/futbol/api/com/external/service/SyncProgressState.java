package futbol.api.com.external.service;

import futbol.api.com.external.dto.Status;
import futbol.api.com.external.dto.SyncProgress;
import futbol.api.com.external.dto.SyncTeamResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static futbol.api.com.external.dto.Status.FAILED;
import static futbol.api.com.external.dto.Status.PARTIAL;
import static futbol.api.com.external.dto.Status.PROCESSING;
import static futbol.api.com.external.dto.Status.SUCCESS;

final class SyncProgressState {

    private Status status = PROCESSING;
    private final List<Integer> leagueIds;
    private final Integer season;
    private int totalTeams;
    private final AtomicInteger processedTeams = new AtomicInteger(0);
    private final AtomicInteger playersCreated = new AtomicInteger(0);
    private final AtomicInteger playersUpdated = new AtomicInteger(0);
    private final AtomicInteger processedLeagues = new AtomicInteger(0);
    private final List<String> errors = new ArrayList<>();
    private final List<SyncTeamResult> teamResults = new ArrayList<>();
    private final LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    SyncProgressState(List<Integer> leagueIds, Integer season) {
        this.leagueIds = List.copyOf(leagueIds);
        this.season = season;
    }

    Status status() {
        return status;
    }

    void apply(LeagueProcessingResult result) {
        totalTeams = result.totalTeams();
        processedTeams.addAndGet(result.processedTeams());
        playersCreated.addAndGet(result.playersCreated());
        playersUpdated.addAndGet(result.playersUpdated());
        errors.addAll(result.errors());
        teamResults.addAll(result.teamResults());
        if (result.leagueProcessed()) {
            processedLeagues.incrementAndGet();
        }
    }

    void markPartial(String error) {
        status = PARTIAL;
        errors.add(error);
    }

    void markFailed(String error) {
        status = FAILED;
        errors.add(error);
    }

    void complete() {
        if (status == PROCESSING) {
            status = errors.isEmpty() ? SUCCESS : PARTIAL;
        }
        completedAt = LocalDateTime.now();
    }

    boolean failed() {
        return status == FAILED;
    }

    boolean hasErrors() {
        return !errors.isEmpty();
    }

    SyncProgress toProgress() {
        List<SyncTeamResult> teams = status == PROCESSING ? null : List.copyOf(teamResults);
        return new SyncProgress(
                status,
                List.copyOf(leagueIds),
                leagueIds.size(),
                processedLeagues.get(),
                totalTeams,
                processedTeams.get(),
                playersCreated.get(),
                playersUpdated.get(),
                season,
                List.copyOf(errors),
                startedAt,
                completedAt,
                teams
        );
    }

    boolean completedBefore(LocalDateTime cutoff) {
        return completedAt != null && completedAt.isBefore(cutoff);
    }
}
