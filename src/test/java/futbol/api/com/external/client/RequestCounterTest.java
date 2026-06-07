package futbol.api.com.external.client;

import futbol.api.com.exceptions.ExternalApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequestCounter Unit Tests")
class RequestCounterTest {

    @Test
    @DisplayName("increment: allows calls up to limit minus safety margin")
    void increment_upToLimit_allowsCalls() {
        RequestCounter counter = new RequestCounter(100);
        for (int i = 0; i < 89; i++) {
            counter.increment();
        }
        assertThat(counter.getCount()).isEqualTo(89);
    }

    @Test
    @DisplayName("increment: throws when approaching limit")
    void increment_overLimit_throws() {
        RequestCounter counter = new RequestCounter(100);
        for (int i = 0; i < 89; i++) {
            counter.increment();
        }
        assertThatThrownBy(counter::increment)
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("rate limit approached");
    }

    @Test
    @DisplayName("increment: configurable daily limit")
    void increment_configurableLimit_allowsMore() {
        RequestCounter counter = new RequestCounter(200);
        for (int i = 0; i < 180; i++) {
            counter.increment();
        }
        assertThat(counter.getCount()).isEqualTo(180);
    }

    @Test
    @DisplayName("increment: configurable limit throws at correct threshold")
    void increment_configurableLimit_throwsAtThreshold() {
        RequestCounter counter = new RequestCounter(50);
        for (int i = 0; i < 39; i++) {
            counter.increment();
        }
        assertThatThrownBy(counter::increment)
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("40");
    }

    @Test
    @DisplayName("remaining: returns correct remaining count")
    void remaining_returnsCorrectCount() {
        RequestCounter counter = new RequestCounter(100);
        assertThat(counter.remaining()).isEqualTo(90);
        counter.increment();
        assertThat(counter.remaining()).isEqualTo(89);
    }

    @Test
    @DisplayName("remaining: with custom limit")
    void remaining_customLimit() {
        RequestCounter counter = new RequestCounter(200);
        assertThat(counter.remaining()).isEqualTo(190);
    }

    @Test
    @DisplayName("reset: resets counter to zero")
    void reset_resetsCounter() {
        RequestCounter counter = new RequestCounter(100);
        counter.increment();
        counter.reset();
        assertThat(counter.getCount()).isZero();
        assertThat(counter.remaining()).isEqualTo(90);
    }
}
