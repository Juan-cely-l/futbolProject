package futbol.api.com.external.config;

import futbol.api.com.external.ApiFootballClient;
import futbol.api.com.external.CachingFootballApiProvider;
import futbol.api.com.external.FootballApiProvider;
import futbol.api.com.external.client.RequestCounter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class FootballApiClientConfig {

    @Bean
    public RestClient footballRestClient(FootballApiConfig config) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-apisports-key", config.apiKey())
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RequestCounter requestCounter(FootballApiConfig config) {
        return new RequestCounter(config.dailyLimit());
    }

    @Bean
    @Primary
    public FootballApiProvider cachingFootballApiProvider(
            ApiFootballClient apiFootballClient,
            ObjectMapper objectMapper,
            @Value("${football.api.cache.dir:${java.io.tmpdir}/futbix-cache}") String cacheDir,
            @Value("${football.api.cache.ttl-hours:24}") long ttlHours
    ) {
        return new CachingFootballApiProvider(
                apiFootballClient,
                objectMapper,
                Path.of(cacheDir),
                Duration.ofHours(ttlHours)
        );
    }
}
