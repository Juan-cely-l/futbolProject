package futbol.api.com.external.mapper;

import futbol.api.com.models.Position;
import org.springframework.stereotype.Component;

@Component
public class MarketValueCalculator {

    private static final long MATCHES_FULL_SEASON = 38;

    private static final int GOAL_WEIGHT_GK = 500_000;
    private static final int GOAL_WEIGHT_DEF = 2_000_000;
    private static final int GOAL_WEIGHT_MID = 1_500_000;
    private static final int GOAL_WEIGHT_FWD = 2_500_000;

    private static final int AST_WEIGHT_GK = 300_000;
    private static final int AST_WEIGHT_DEF = 1_000_000;
    private static final int AST_WEIGHT_MID = 1_500_000;
    private static final int AST_WEIGHT_FWD = 1_200_000;

    private static final int MAT_WEIGHT_GK = 100_000;
    private static final int MAT_WEIGHT_DEF = 150_000;
    private static final int MAT_WEIGHT_MID = 100_000;
    private static final int MAT_WEIGHT_FWD = 75_000;

    private static final long BASE_GK = 8_000_000L;
    private static final long BASE_DEF = 8_000_000L;
    private static final long BASE_MID = 12_000_000L;
    private static final long BASE_FWD = 15_000_000L;

    public int calculate(Position position, int age, int goals, int assists, int matches) {
        long base = switch (position) {
            case GOALKEEPER -> BASE_GK;
            case DEFENDER -> BASE_DEF;
            case MIDFIELDER -> BASE_MID;
            case FORWARD -> BASE_FWD;
        };

        int goalWeight = switch (position) {
            case GOALKEEPER -> GOAL_WEIGHT_GK;
            case DEFENDER -> GOAL_WEIGHT_DEF;
            case MIDFIELDER -> GOAL_WEIGHT_MID;
            case FORWARD -> GOAL_WEIGHT_FWD;
        };

        int astWeight = switch (position) {
            case GOALKEEPER -> AST_WEIGHT_GK;
            case DEFENDER -> AST_WEIGHT_DEF;
            case MIDFIELDER -> AST_WEIGHT_MID;
            case FORWARD -> AST_WEIGHT_FWD;
        };

        int matWeight = switch (position) {
            case GOALKEEPER -> MAT_WEIGHT_GK;
            case DEFENDER -> MAT_WEIGHT_DEF;
            case MIDFIELDER -> MAT_WEIGHT_MID;
            case FORWARD -> MAT_WEIGHT_FWD;
        };

        double matchesRatio = Math.min(1.0, Math.max(0, matches) / (double) MATCHES_FULL_SEASON);
        double matchesFactor = 0.3 + 0.7 * matchesRatio;

        long statBonus = (long) ((goals * goalWeight + assists * astWeight + matches * matWeight) * matchesFactor);

        double ageFactor = ageFactor(position, age);

        return (int) ((base + statBonus) * ageFactor);
    }

    static double ageFactor(Position position, int age) {
        int adjustedAge = position == Position.GOALKEEPER ? age - 2 : age;

        if (adjustedAge < 17) return 0.40;
        if (adjustedAge < 20) return 0.50 + (adjustedAge - 17) * 0.117;
        if (adjustedAge < 24) return 0.85 + (adjustedAge - 20) * 0.0375;
        if (adjustedAge <= 28) return 1.0;
        if (adjustedAge < 32) return 1.0 - (adjustedAge - 28) * 0.0625;
        if (adjustedAge < 36) return 0.81 - (adjustedAge - 31) * 0.1275;
        return Math.max(0.08, 0.30 - (adjustedAge - 35) * 0.055);
    }
}
