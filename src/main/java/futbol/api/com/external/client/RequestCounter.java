package futbol.api.com.external.client;

import futbol.api.com.exceptions.ExternalApiException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestCounter {

    private static final Integer SAFETY_MARGIN = 10;

    private final Integer dailyLimit;
    private final AtomicInteger counter = new AtomicInteger(0);
    private LocalDate lastResetDate = LocalDate.now();

    public RequestCounter(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public synchronized void increment() {
        refreshIfNeeded();
        int current = counter.incrementAndGet();
        if (current >= dailyLimit - SAFETY_MARGIN) {
            throw new ExternalApiException(429,
                    "API-Football rate limit approached: " + current + "/" + dailyLimit + " requests used");
        }
    }

    public synchronized int getCount() {
        refreshIfNeeded();
        return counter.get();
    }

    public synchronized boolean isLimitReached() {
        refreshIfNeeded();
        return counter.get() >= dailyLimit - SAFETY_MARGIN;
    }

    public synchronized int remaining() {
        refreshIfNeeded();
        return (dailyLimit - SAFETY_MARGIN) - counter.get();
    }

    public synchronized void reset() {
        counter.set(0);
        lastResetDate = LocalDate.now();
    }

    private void refreshIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            counter.set(0);
            lastResetDate = today;
        }
    }
}
