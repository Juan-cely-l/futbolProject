package futbol.api.com.external.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
