## Why

The current sync progress only exposes aggregate counters (total teams, processed teams, players created/updated). Users cannot see which teams and players were actually synchronized — what was created vs. updated, which players ended up in which team, or what the final squad looks like after sync. This makes the sync feature feel like a black box: "10 teams processed" tells the user nothing about what changed.

Adding per-team and per-player result data to the final sync progress gives users transparency into exactly what the sync operation produced.

## What Changes

- **New DTOs**: `SyncTeamResult` and `SyncPlayerResult` records to capture per-team and per-player sync outcomes
- **SyncProgress modified**: New `teams` field (`List<SyncTeamResult>`) populated on sync completion
- **ExternalFootballService modified**: `SyncStats` gains tracking for per-team results; `processTeam()` builds `SyncTeamResult` with full squad data; `getProgress()` returns team results only after processing completes
- **SyncContext.jsx modified**: Captures `teams` array from final progress response
- **SyncModal.jsx modified**: Adds expandable accordion view showing synced teams and their full player squads

## Capabilities

### New Capabilities
- `sync-team-results`: Expose per-team and per-player results from an API sync operation, viewable in the sync modal as an expandable accordion showing teams with their complete squads (position, age, goals, assists, market value)

### Modified Capabilities
- *(none — no existing specs are affected)*

## Impact

- **Backend — New files**: `SyncTeamResult.java`, `SyncPlayerResult.java` DTO records
- **Backend — Modified**: `SyncProgress.java` (adds `teams` field), `ExternalFootballService.java` (build and expose per-team results), tests
- **Frontend — Modified**: `SyncContext.jsx` (capture `teams`), `SyncModal.jsx` (render accordion with team/player data)
- **No API breakage**: The new `teams` field in `SyncProgress` is additive — existing consumers are unaffected
- **No new dependencies**: All data already exists in the database; no additional API calls needed
