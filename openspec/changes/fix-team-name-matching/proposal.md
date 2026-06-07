## Why

Teams created via seed data (e.g., "arsenal fc") don't match API-Football team names (e.g., "Arsenal") during external sync, causing duplicate teams with 0 players. Additionally, the rate-limit budget is underestimated for teams with large squads, causing mid-sync failures that leave teams with no players.

## What Changes

- Normalize team names in `FootballDataMapper.toTeamInfo()` by stripping common suffixes (fc, cf, ud, afc, ac) before matching
- Normalize team names in `DataSeeder` to save bare names (without suffixes) so seed and API are consistent from the start
- Fix `ESTIMATED_REQUESTS_PER_TEAM` constant to account for paginated player fetches
- Cache `getTeamsByLeague()` result to avoid double API call per league

## Capabilities

### New Capabilities
- `external-team-name-normalization`: Normalize football club name suffixes (FC, CF, UD, etc.) in the external API integration layer so team matching works regardless of naming conventions
- `api-rate-limit-budget`: Accurately estimate API request budget per team and per league to prevent mid-sync failures

### Modified Capabilities

*(No existing specs are modified — requirements of existing capabilities don't change.)*

## Impact

- **`src/main/java/futbol/api/com/external/mapper/FootballDataMapper.java`**: Add suffix normalization in `toTeamInfo()`
- **`src/main/java/futbol/api/com/seed/DataSeeder.java`**: Normalize team names at seed time to bare names
- **`src/main/java/futbol/api/com/external/service/ExternalFootballService.java`**: Fix rate-limit estimate and cache team list
- **`src/main/java/futbol/api/com/external/ApiFootballClient.java`**: May need minor adjustment for page count exposure
