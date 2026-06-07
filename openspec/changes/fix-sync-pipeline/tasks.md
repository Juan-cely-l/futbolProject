## 1. Add league param to player endpoint

- [x] 1.1 Add `leagueId` parameter to `ApiFootballClient.getPlayersByTeam()` and append `&league=` to the URI
- [x] 1.2 Update `ExternalFootballService.processTeam()` to pass `leagueId` through `getPlayersByTeam()`
- [x] 1.3 Update `CachingFootballApiProvider` and `FootballApiProvider` interface to accept `leagueId`
- [ ] 1.4 Verify squad sizes drop to 25-35 per team

## 2. Split transactions in processTeam

- [x] 2.1 Move player fetch + save + budget recalculation into a separate transactional method
- [x] 2.2 Keep only team find-or-create in the original transaction
- [x] 2.3 Ensure team survives even if player step fails (minimum budget if no players)

## 3. Configurable rate limit

- [x] 3.1 Add `dailyLimit` field to `FootballApiConfig` with property `football.api.daily-limit` (already existed)
- [x] 3.2 Update `RequestCounter` to accept limit via constructor (remove hardcoded 100)
- [x] 3.3 Wire `RequestCounter` with config value via @Bean in `FootballApiClientConfig`

## 4. Fix budget persistence

- [x] 4.1 Add `teamRepository.save(team)` after `team.setBudget(budget)` in the player processing method

## 5. Update tests

- [x] 5.1 Update `ExternalFootballServiceTest` — updated budget test, added team-survives test
- [x] 5.2 Create `RequestCounterTest` to verify configurable limit
- [x] 5.3 Verify all tests pass
