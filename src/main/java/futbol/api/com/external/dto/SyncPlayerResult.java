package futbol.api.com.external.dto;

public record SyncPlayerResult(
        String name,
        String position,
        Integer age,
        String photo,
        Integer goals,
        Integer assists,
        Integer matches,
        Integer valueMarket
) {
}
