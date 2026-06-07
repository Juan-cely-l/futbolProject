## Why

Users can only sync one hardcoded league (La Liga, season 2024) at a time. The daily API budget (100 requests) covers ~5 leagues, but there's no UI to select which leagues or which season to sync. Adding league and season selection gives users control over what data they import and lets them use the full daily budget efficiently.

## What Changes

- **POST /futbix/v1/sync** now accepts JSON body `{ leagueIds, season }` instead of a single `leagueId` query param
- **GET /futbix/v1/sync/leagues** new endpoint returning available leagues from config
- **GET /futbix/v1/sync/seasons** new endpoint returning min/max/current season range
- **SyncModal** new frontend component with league checkboxes and season dropdown
- **Navbar** Sync button opens modal instead of launching sync directly
- **ExternalFootballService** iterates selected leagues serially, respects daily rate limit
- **RequestCounter** adds `remaining()` method and daily auto-reset
- **AsyncConfig** new named executor `footballSyncExecutor` (pool=1)
- **SyncProgress** expanded with `leagueIds`, `totalLeagues`, `processedLeagues`, `season`
- **Thread safety**: `SyncStats` fields become `AtomicInteger`

## Capabilities

### New Capabilities
- `sync-configuration`: Exposes available leagues and seasons via API; provides a frontend dialog for users to select which leagues and season to sync
- `sync-execution`: Executes sync serially across selected leagues, respecting the daily API budget, with expanded progress reporting

### Modified Capabilities
None — no existing sync specs.

## Impact

- **Backend**: SyncController, ExternalFootballService, SyncProgress, RequestCounter, AsyncConfig (new), FootballApiConfig
- **Frontend**: SyncModal (new), SyncContext, Navbar, external.js
- **Data model**: No changes to Player/Team entities
- **Config**: New properties `football.api.season-min`, `football.api.season-max`
- **Deployment**: Backward-compatible (old query param still works, new JSON body preferred)
