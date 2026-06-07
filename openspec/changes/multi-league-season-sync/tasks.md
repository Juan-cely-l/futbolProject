## 1. Backend config & DTOs

- [x] 1.1 Add `season-min` and `season-max` properties to `application.properties` and `FootballApiConfig`
- [x] 1.2 Create `SyncRequest` record with `List<Integer> leagueIds` and `Integer season`
- [x] 1.3 Create `LeaguesResponse` and `SeasonsResponse` DTOs for new endpoints
- [x] 1.4 Update `SyncProgress` record: replace single `leagueId` with `List<Integer> leagueIds`, add `totalLeagues`, `processedLeagues`, `Integer season`
- [x] 1.5 Create `AsyncConfig.java` with `@Bean("footballSyncExecutor")` (pool=1, queue=10)

## 2. Backend endpoints & service

- [x] 2.1 Update `SyncController`: POST accepts JSON body, add `GET /sync/leagues` and `GET /sync/seasons`
- [x] 2.2 Add league name mapping (static map: ID → "Premier League", etc.) in service
- [x] 2.3 Update `ExternalFootballService.syncAll()` to accept `List<Integer> leagueIds` and `Integer season`
- [x] 2.4 Refactor `executeSync()` to iterate leagues serially, passing season to API calls
- [x] 2.5 Add rate limit pre-check before each league (estimate budget, skip if insufficient)
- [x] 2.6 Add graceful stop on rate limit — catch 429 at league level, set PARTIAL
- [x] 2.7 Add `remaining()` method to `RequestCounter` and daily auto-reset

## 3. Backend thread safety & cleanup

- [x] 3.1 Change `SyncStats` `processedTeams`, `playersCreated`, `playersUpdated` to `AtomicInteger`
- [x] 3.2 Add `processedLeagues` AtomicInteger field to SyncStats
- [x] 3.3 Add `@Async("footballSyncExecutor")` annotation on `executeSync()`
- [x] 3.4 Add `@EnableScheduling` and scheduled progress map eviction (every 5 min, remove entries older than 30 min)
- [x] 3.5 Add 409 Conflict guard for concurrent sync requests

## 4. Frontend API & context

- [x] 4.1 Update `api/external.js`: change `triggerSync()` to POST JSON body, add `fetchSyncLeagues()`, `fetchSyncSeasons()`
- [x] 4.2 Update `SyncContext.jsx`: support leagueIds and season params, persist league selection to sessionStorage

## 5. Frontend UI

- [x] 5.1 Create `SyncModal.jsx` with league checkboxes, season dropdown, and progress display
- [x] 5.2 Update `Navbar.jsx`: Sync button opens modal instead of calling `startSync()` directly
- [x] 5.3 Wire modal to SyncContext: pass selected leagues/season, show progress, handle completion

## 6. Verify

- [ ] 6.1 Rebuild Docker containers and verify backend endpoints (`/sync/leagues`, `/sync/seasons`, `POST /sync`)
- [ ] 6.2 Open frontend and verify sync modal opens, leagues/season load, sync runs with selected params
- [ ] 6.3 Verify rate limit pre-check works (trigger sync near budget limit)
- [ ] 6.4 Verify 409 response when triggering sync while another is in progress
