package futbol.api.com.external.mapper;

import futbol.api.com.models.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketValueCalculator Unit Tests")
class MarketValueCalculatorTest {

    private final MarketValueCalculator calculator = new MarketValueCalculator();

    // ─── Base values (peak age, no stats) ──────────────────────

    @Test
    @DisplayName("calculate: FORWARD base value at peak age")
    void calculate_forwardPeakAge_noStats() {
        int value = calculator.calculate(Position.FORWARD, 26, 0, 0, 0);
        assertThat(value).isEqualTo(15_000_000);
    }

    @Test
    @DisplayName("calculate: MIDFIELDER base value at peak age")
    void calculate_midfielderPeakAge_noStats() {
        int value = calculator.calculate(Position.MIDFIELDER, 26, 0, 0, 0);
        assertThat(value).isEqualTo(12_000_000);
    }

    @Test
    @DisplayName("calculate: DEFENDER base value at peak age")
    void calculate_defenderPeakAge_noStats() {
        int value = calculator.calculate(Position.DEFENDER, 26, 0, 0, 0);
        assertThat(value).isEqualTo(8_000_000);
    }

    @Test
    @DisplayName("calculate: GOALKEEPER base value at peak age")
    void calculate_goalkeeperPeakAge_noStats() {
        int value = calculator.calculate(Position.GOALKEEPER, 26, 0, 0, 0);
        assertThat(value).isEqualTo(8_000_000);
    }

    // ─── Deterministic ─────────────────────────────────────────

    @Test
    @DisplayName("calculate: identical inputs produce identical outputs")
    void calculate_identicalInputs_identicalOutputs() {
        int first = calculator.calculate(Position.FORWARD, 24, 35, 8, 35);
        int second = calculator.calculate(Position.FORWARD, 24, 35, 8, 35);
        assertThat(first).isEqualTo(second);
    }

    // ─── Age factor ────────────────────────────────────────────

    @Test
    @DisplayName("calculate: age 26 produces peak value for non-GK")
    void calculate_age26_peakFactor() {
        int peak = calculator.calculate(Position.FORWARD, 26, 0, 0, 0);
        int younger = calculator.calculate(Position.FORWARD, 20, 0, 0, 0);
        int older = calculator.calculate(Position.FORWARD, 32, 0, 0, 0);
        assertThat(younger).isLessThan(peak);
        assertThat(older).isLessThan(peak);
    }

    @Test
    @DisplayName("calculate: FORWARD at age 17 gets reduced factor")
    void calculate_age17_reducedFactor() {
        int value = calculator.calculate(Position.FORWARD, 17, 0, 0, 0);
        assertThat(value).isEqualTo(7_500_000);
    }

    @Test
    @DisplayName("calculate: FORWARD at age 35 gets reduced factor")
    void calculate_age35_reducedFactor() {
        int value = calculator.calculate(Position.FORWARD, 35, 0, 0, 0);
        assertThat(value).isEqualTo(4_500_000);
    }

    @Test
    @DisplayName("calculate: GK at age 28 gets peak factor (+2 year shift)")
    void calculate_goalkeeperAge28_peakFactor() {
        // GK adjusted age = 26 (peak), FWD adjusted age = 28 (peak)
        int value = calculator.calculate(Position.GOALKEEPER, 28, 0, 0, 0);
        assertThat(value).isEqualTo(8_000_000);  // GK peak at adjusted 26
    }

    @Test
    @DisplayName("calculate: GK age 32 has higher age factor than GK age 35 (+2 year shift)")
    void calculate_goalkeeperAge32_higherThanOlder() {
        int gk32 = calculator.calculate(Position.GOALKEEPER, 32, 0, 0, 0);
        int gk35 = calculator.calculate(Position.GOALKEEPER, 35, 0, 0, 0);
        // GK 32: adjusted=30, factor=0.875, value=7M
        // GK 35: adjusted=33, factor=0.81-(33-31)*0.1275=0.555, value=4.44M
        assertThat(gk32).isGreaterThan(gk35);
    }

    @Test
    @DisplayName("calculate: FWD age 32 has lower factor than GK age 32 due to +2 year GK shift")
    void calculate_goalkeeperAge32_higherFactorThanFwd32() {
        // Direct factor comparison: positions differ, but we can verify ratio
        int gkPeak = calculator.calculate(Position.GOALKEEPER, 26, 0, 0, 0);
        int gk32 = calculator.calculate(Position.GOALKEEPER, 32, 0, 0, 0);
        int fwdPeak = calculator.calculate(Position.FORWARD, 26, 0, 0, 0);
        int fwd32 = calculator.calculate(Position.FORWARD, 32, 0, 0, 0);
        // GK decline from peak: 8M -> 7M = -12.5%
        // FWD decline from peak: 15M -> 10.24M = -31.7%
        double gkRatio = (double) gk32 / gkPeak;
        double fwdRatio = (double) fwd32 / fwdPeak;
        assertThat(gkRatio).isGreaterThan(fwdRatio);
    }

    @Test
    @DisplayName("calculate: extreme age 40 gets near-minimum factor")
    void calculate_age40_nearMinimumFactor() {
        int value = calculator.calculate(Position.FORWARD, 40, 0, 0, 0);
        // adjustedAge=40, factor = 0.30 - (40-35)*0.055 = 0.30 - 0.275 = 0.025 -> max(0.08, 0.025) = 0.08
        assertThat(value).isEqualTo((int)(15_000_000 * 0.08));
    }

    // ─── Stat bonuses (position weights) ───────────────────────

    @Test
    @DisplayName("calculate: goals increase value for all positions")
    void calculate_goals_increaseValue() {
        int without = calculator.calculate(Position.FORWARD, 26, 0, 0, 38);
        int with = calculator.calculate(Position.FORWARD, 26, 10, 0, 38);
        assertThat(with).isGreaterThan(without);
    }

    @Test
    @DisplayName("calculate: assists increase value")
    void calculate_assists_increaseValue() {
        int without = calculator.calculate(Position.MIDFIELDER, 26, 0, 0, 38);
        int with = calculator.calculate(Position.MIDFIELDER, 26, 0, 10, 38);
        assertThat(with).isGreaterThan(without);
    }

    @Test
    @DisplayName("calculate: defender goals are weighted more heavily than midfielder goals")
    void calculate_defenderGoalWeight_higherThanMidfielder() {
        int def = calculator.calculate(Position.DEFENDER, 26, 5, 0, 38);
        int mid = calculator.calculate(Position.MIDFIELDER, 26, 5, 0, 38);
        // DEF goal weight 2M > MID goal weight 1.5M, but DEF base 8M < MID base 12M
        int defNoStats = calculator.calculate(Position.DEFENDER, 26, 0, 0, 38);
        int midNoStats = calculator.calculate(Position.MIDFIELDER, 26, 0, 0, 38);
        int defIncrease = def - defNoStats;
        int midIncrease = mid - midNoStats;
        assertThat(defIncrease).isGreaterThan(midIncrease);
    }

    // ─── Matches factor ────────────────────────────────────────

    @Test
    @DisplayName("calculate: full season matches get full stat bonus")
    void calculate_38matches_fullBonus() {
        int fullSeason = calculator.calculate(Position.FORWARD, 26, 10, 0, 38);
        int zeroMatches = calculator.calculate(Position.FORWARD, 26, 10, 0, 0);
        assertThat(zeroMatches).isLessThan(fullSeason);
    }

    @Test
    @DisplayName("calculate: zero matches still gets base value")
    void calculate_zeroMatches_baseValue() {
        int value = calculator.calculate(Position.MIDFIELDER, 26, 0, 0, 0);
        assertThat(value).isEqualTo(12_000_000);
    }

    // ─── High-value players ────────────────────────────────────

    @Test
    @DisplayName("calculate: world-class forward at peak age")
    void calculate_worldClassForward() {
        int value = calculator.calculate(Position.FORWARD, 24, 30, 12, 35);
        assertThat(value).isBetween(80_000_000, 120_000_000);
    }

    @Test
    @DisplayName("calculate: elite midfielder at peak age")
    void calculate_eliteMidfielder() {
        int value = calculator.calculate(Position.MIDFIELDER, 26, 10, 15, 34);
        assertThat(value).isBetween(35_000_000, 70_000_000);
    }

    @Test
    @DisplayName("calculate: veteran goalkeeper moderate value")
    void calculate_veteranGoalkeeper() {
        int value = calculator.calculate(Position.GOALKEEPER, 35, 0, 0, 32);
        // GK adjusted=33, factor = 0.81 - (33-31)*0.1275 = 0.81-0.255 = 0.555
        // value = (8M + (32*100K * matchesFactor)) * 0.555
        // matchesRatio = 32/38 = 0.842, matchesFactor = 0.3+0.7*0.842 = 0.889
        // matchesBonus = (32*100K) * 0.889 = 2_844_800
        // (8M + 2.84M) * 0.555 = ~6M
        assertThat(value).isBetween(4_000_000, 9_000_000);
    }

    // ─── Edge cases ────────────────────────────────────────────

    @Test
    @DisplayName("calculate: all positions produce non-negative values")
    void calculate_allPositions_nonNegative() {
        for (Position pos : Position.values()) {
            for (int age = 16; age <= 45; age += 5) {
                int value = calculator.calculate(pos, age, 0, 0, 0);
                assertThat(value).as("Position %s age %d", pos, age).isNotNegative();
            }
        }
    }

    @Test
    @DisplayName("calculate: high stats produce realistic top-end value")
    void calculate_highStats_realisticTopEnd() {
        // Exceptional forward: 40 goals, 15 assists, 38 matches, age 24
        int value = calculator.calculate(Position.FORWARD, 24, 40, 15, 38);
        assertThat(value).isGreaterThan(100_000_000);
        assertThat(value).isLessThan(200_000_000);
    }
}
