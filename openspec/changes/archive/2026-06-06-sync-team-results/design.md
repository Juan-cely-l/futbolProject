## Context

The sync system currently returns aggregate progress counters (totalTeams, processedTeams, playersCreated, playersUpdated) with no per-item granularity. The frontend shows a progress bar and summary numbers, but users cannot see *which* teams were synced or what their squads look like afterwards.

The data already exists in the database after sync — `Team` and `Player` entities are persisted per-team inside `transactionTemplate.execute()`. We can query them back immediately after processing each team, avoiding extra API calls to the external football API.

## Goals / Non-Goals

**Goals:**
- Expose per-team sync results (team name, country, created/updated status, creation/update timestamps) in the final sync progress
- Expose each team's full player squad (name, position, age, goals, assists, matches, market value) in the sync results
- Show these results in the frontend sync modal as an expandable accordion
- Keep data within the same backend response payload (no new endpoints)

**Non-Goals:**
- Real-time per-team updates during sync (teams array is only populated at the end)
- Virtualized/scrolling for large result sets (deferred to future perf work)
- Changing the existing aggregate progress counters
- Exposing per-team errors (errors remain at the aggregate level)

## Decisions

1. **Query players from DB vs. holding in memory**: Query `playerRepository.findPlayersByTeam_Name(name)` at the end of each `processTeam()` call. This uses an indexed query, avoids storing player objects in mutable `SyncStats` memory, and guarantees the data is consistent with what was actually persisted.

2. **SyncTeamResult as a Java record**: Follows the existing pattern of `SyncProgress`, `SyncRequest`, `SyncResult` as immutable records. No Lombok, no JPA annotations — pure data carriers.

3. **Null `teams` during processing, populated at completion**: The `SyncProgress.teams` field is `null` while `status == PROCESSING` and populated only when `status != PROCESSING`. This avoids sending large payloads on every poll (every 5 seconds) and is a clear semantic signal to the frontend.

4. **Accordion in SyncModal instead of new page**: The team results are consumed once after sync completes. An accordion within the existing modal avoids context switches and keeps the flow contained. Virtualization can be added later if performance degrades.

5. **Skip SyncResult.java integration**: The existing `SyncResult` record is dead code and serves a different purpose (aggregate summary, not per-item detail). We leave it as-is rather than retrofitting it.

## Risks / Trade-offs

- **Payload size**: ~2500 player objects across ~38 teams is ~400KB as JSON. Mitigation: this is a single response after sync completes, not repeated polling. If latency becomes an issue, add pagination or virtual scrolling in a follow-up.
- **Partial sync**: If a league fails mid-way, `teamResults` only includes successfully processed teams. The frontend will show whatever was completed. This is correct behavior, not a bug.
- **Empty results**: If 0 teams are synced (all failed or no data), the `teams` list is empty. Frontend shows "No teams synced" banner.
- **totalTeams remains per-league**: The existing `totalTeams` field continues to be overwritten per league. We do not fix this in this change — out of scope.
