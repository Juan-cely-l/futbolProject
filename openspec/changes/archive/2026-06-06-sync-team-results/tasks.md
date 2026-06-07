## 1. Backend — New sync result DTOs

- [x] 1.1 Create `SyncTeamResult.java` record — fields: name, country, created (boolean), updated (boolean), players (List<SyncPlayerResult>)
- [x] 1.2 Create `SyncPlayerResult.java` record — fields: name, position (String), age, photo, goals, assists, matches, valueMarket

## 2. Backend — Modify SyncProgress.java

- [x] 2.1 Add `List<SyncTeamResult> teams` field (nullable — null during PROCESSING, populated on completion)

## 3. Backend — Modify ExternalFootballService.java

- [x] 3.1 Add `List<SyncTeamResult> teamResults` field to inner `SyncStats` class
- [x] 3.2 Inject `PlayerRepository` into `ExternalFootballService` (or use existing reference)
- [x] 3.3 Modify `processTeam()`: after `transactionTemplate.execute()`, query `playerRepository.findPlayersByTeam_Name(name)` and build `SyncTeamResult` with full player squad; add to `stats.teamResults`
- [x] 3.4 Modify `getProgress()`: when `status != PROCESSING`, pass `stats.teamResults` as the SyncProgress teams field; otherwise pass `null`

## 4. Backend — Update tests

- [x] 4.1 Update `ExternalFootballServiceTest`: verify team results are populated in SyncProgress after sync completes; verify team results contain correct player data
- [x] 4.2 Update `SyncControllerTest`: verify the `teams` field appears in the GET /{syncId} response

## 5. Frontend — Modify SyncContext.jsx

- [x] 5.1 Add `teams` to the `progress` state shape
- [x] 5.2 Extract `result.teams` from the final sync polling response

## 6. Frontend — Modify SyncModal.jsx

- [x] 6.1 Add expandable accordion component for synced teams, shown only after sync completes
- [x] 6.2 Each team row shows: name, country, created/updated badge
- [x] 6.3 Expanding a team reveals its player list: position, age, goals, assists, market value
- [x] 6.4 Show "No teams synced" message when teams list is empty
