package futbol.api.com.external.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequestCounter Additional Tests")
class RequestCounterAdditionalTest {

    private RequestCounter counter;

    @BeforeEach
    void setUp() {
        counter = new RequestCounter(100);
    }

    @Test
    @DisplayName("reset: resets count to zero")
    void reset_clearsCount() {
        counter.increment();
        counter.increment();
        assertThat(counter.getCount()).isEqualTo(2);

        counter.reset();
        assertThat(counter.getCount()).isZero();
    }

    @Test
    @DisplayName("remaining: returns correct remaining count")
    void remaining_returnsCorrectCount() {
        assertThat(counter.remaining()).isEqualTo(90);
        counter.increment();
        assertThat(counter.remaining()).isEqualTo(89);
    }

    @Test
    @DisplayName("isLimitReached: false when under limit")
    void isLimitReached_falseWhenUnder() {
        assertThat(counter.isLimitReached()).isFalse();
    }

    @Test
    @DisplayName("read methods: refresh rollover state before any new increment")
    void readMethods_refreshStateAfterDayRollover() throws Exception {
        counter.increment();
        counter.increment();
        setLastResetDate(counter, LocalDate.now().minusDays(1));

        assertThat(counter.getCount()).isZero();
        assertThat(counter.remaining()).isEqualTo(90);
        assertThat(counter.isLimitReached()).isFalse();
    }

    @Test
    @DisplayName("increment: resets stale state before counting current day")
    void increment_refreshesStateAfterDayRollover() throws Exception {
        counter.increment();
        counter.increment();
        setLastResetDate(counter, LocalDate.now().minusDays(1));

        counter.increment();

        assertThat(counter.getCount()).isEqualTo(1);
        assertThat(counter.remaining()).isEqualTo(89);
    }

    @Test
    @DisplayName("reset: clears threshold state and restores full allowance")
    void reset_clearsThresholdState() {
        for (int i = 0; i < 89; i++) {
            counter.increment();
        }

        counter.reset();

        assertThat(counter.getCount()).isZero();
        assertThat(counter.isLimitReached()).isFalse();
        assertThat(counter.remaining()).isEqualTo(90);
    }

    @Test
    @DisplayName("threshold: increment that reaches safety threshold is blocked and marks limit reached")
    void threshold_incrementThatReachesSafetyThresholdIsBlocked() {
        for (int i = 0; i < 89; i++) {
            counter.increment();
        }

        assertThat(counter.isLimitReached()).isFalse();
        assertThat(counter.remaining()).isEqualTo(1);

        assertThatThrownBy(counter::increment)
                .hasMessageContaining("rate limit approached");

        assertThat(counter.getCount()).isEqualTo(90);
        assertThat(counter.isLimitReached()).isTrue();
        assertThat(counter.remaining()).isZero();
    }

    private static void setLastResetDate(RequestCounter counter, LocalDate date) throws Exception {
        Field field = RequestCounter.class.getDeclaredField("lastResetDate");
        field.setAccessible(true);
        field.set(counter, date);
    }
}
