## 1. Name Normalization

- [x] 1.1 Create `TeamNameNormalizer` utility class with a static `normalize(String name)` method that strips common suffixes (fc, cf, ud, afc, ac) from team names
- [x] 1.2 Update `FootballDataMapper.toTeamInfo()` to call `TeamNameNormalizer.normalize()` on the API team name before creating `TeamInfo`
- [x] 1.3 Update `DataSeeder.runSeed()` to call `TeamNameNormalizer.normalize()` on each team name before persisting

## 2. Rate Limit Budget

- [x] 2.1 Change `ESTIMATED_REQUESTS_PER_TEAM` from `1L` to `5L` in `ExternalFootballService`
- [ ] 2.2 Optional: Expose page count from `ApiFootballClient.getPlayersByTeam()` and use actual pages in the estimate (deferred — fixed constant is sufficient for now)

## 3. Eliminate Double Team-List Fetch

- [x] 3.1 Refactor `estimateRequestsForLeague()` to return both the estimated count and the fetched `List<TeamData>` (e.g., as a record)
- [x] 3.2 Change `processLeague()` to accept `List<TeamData>` as a parameter instead of calling `getTeamsByLeague()` internally
- [x] 3.3 Update `executeSync()` to pass the cached team list from the estimate phase to `processLeague()`

## 4. Tests

- [x] 4.1 Write unit tests for `TeamNameNormalizer.normalize()` covering: FC suffix, CF suffix, no suffix, bare-minimum names ("AC Milan" edge case), multi-word names
- [x] 4.2 Update `ExternalFootballService` tests to cover the new `ESTIMATED_REQUESTS_PER_TEAM` value and cached team list flow
- [x] 4.3 Run all backend tests and verify 0 failures
