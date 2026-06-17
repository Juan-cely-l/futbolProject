package futbol.api.com.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("application-prod.properties Tests")
class ApplicationProdPropertiesTest {

    @Test
    @DisplayName("production properties: enable framework forwarded headers and trusted proxies")
    void productionProperties_includeForwardedHeadersStrategyAndTrustedProxies() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-prod.properties")) {
            assertThat(input).isNotNull();
            properties.load(input);
        }

        assertThat(properties.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
        assertThat(properties.getProperty("rate-limit.trusted-proxies")).isNotBlank();
    }
}
