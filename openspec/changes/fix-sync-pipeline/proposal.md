## Why

After syncing external API data, some teams are created with 0 squad and €5M minimum budget while others have 50-60 players. The root cause is a rate limit that's reached mid-sync, causing transaction rollback for remaining teams. Additionally, player counts are inflated because the API endpoint returns players from all competitions instead of just the league.

## What Changes

- Add `league` parameter to the `/players` API endpoint to filter by league competition
- Separate team creation from player fetch into distinct transaction boundaries
- Fix `RequestCounter` to read daily limit from configuration instead of hardcoding
- Move rate limit check before counter increment to fail gracefully per-team
- Fix budget to persist after recalculation
- Update test for the player endpoint change

## Capabilities

### New Capabilities
- `sync-pipeline-resilience`: Robust sync pipeline with proper transaction boundaries, rate limiting, and player filtering by league

### Modified Capabilities
- *None* — `external-api` is implementation-internal (no public spec)

## Impact

- `src/main/java/futbol/api/com/external/ApiFootballClient.java` — Add league filter to player endpoint URI
- `src/main/java/futbol/api/com/external/mapper/FootballDataMapper.java` — Pass league ID through mapping
- `src/main/java/futbol/api/com/external/service/ExternalFootballService.java` — Split transactions, pass league context
- `src/main/java/futbol/api/com/external/client/RequestCounter.java` — Read limit from config
- `src/main/java/futbol/api/com/external/config/FootballApiConfig.java` — Expose daily limit property
- Affected tests: `ExternalFootballServiceTest`, `CachingFootballApiProviderTest`
- No frontend changes needed
