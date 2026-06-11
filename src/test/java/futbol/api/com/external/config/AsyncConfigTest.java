package futbol.api.com.external.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AsyncConfig Unit Tests")
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    @DisplayName("footballSyncExecutor: creates executor with pool size 1")
    void footballSyncExecutor_createsExecutor() {
        Executor executor = config.footballSyncExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(1);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(1);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("football-sync-");
    }
}
