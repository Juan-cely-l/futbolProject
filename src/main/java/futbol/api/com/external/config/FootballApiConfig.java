package futbol.api.com.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "football.api")
public record FootballApiConfig (
        String apiKey,
        String apiHost,
        String baseUrl,
        Integer season,
        Integer seasonMin,
        Integer seasonMax,
        Integer dailyLimit,
        List<Integer> leagueIds
)
{}
