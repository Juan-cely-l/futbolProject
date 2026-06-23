package futbol.api.com.external.service;

import java.util.Map;

final class SyncConstants {

    static final long DELAY_BETWEEN_TEAMS_MS = 6_000L;
    static final long ESTIMATED_REQUESTS_PER_TEAM = 5L;
    static final long ESTIMATED_REQUESTS_PER_LEAGUE_FIXED = 1L;
    static final long MINIMUM_BUDGET = 5_000_000L;

    static final Map<Integer, Double> BUDGET_TO_SQUAD_RATIO = Map.of(
            39, 0.50,
            140, 0.40,
            135, 0.35,
            78, 0.35,
            61, 0.30
    );

    static final Map<Integer, String> LEAGUE_NAMES = Map.of(
            39, "Premier League",
            140, "La Liga",
            135, "Serie A",
            78, "Bundesliga",
            61, "Ligue 1"
    );

    private SyncConstants() {
    }
}
