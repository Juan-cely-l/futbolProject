package futbol.api.com.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Nginx Config Tests")
class NginxConfigTest {

    @Test
    @DisplayName("nginx.conf: defines security and forwarded proxy headers")
    void nginxConfig_containsSecurityAndForwardedHeaders() throws Exception {
        String config = Files.readString(Path.of("frontend/nginx.conf"));

        assertThat(config).contains("Strict-Transport-Security");
        assertThat(config).contains("Content-Security-Policy");
        assertThat(config).contains("proxy_set_header X-Forwarded-For");
        assertThat(config).contains("proxy_set_header X-Forwarded-Proto");
        assertThat(config).contains("proxy_set_header X-Real-IP");
    }
}
