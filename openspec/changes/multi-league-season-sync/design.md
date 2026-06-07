## Context

The current sync system supports a single league (`leagueId=140`, La Liga) with a hardcoded season (`football.api.season=2024`). The API-Football provider has 5 leagues configured and supports multiple seasons. The daily API limit is 100 requests.

The user has no way to select which leagues or season to sync. This design covers adding that selection capability and executing syncs across multiple leagues while respecting the rate limit.

Key constraints: 100 requests/day API budget, ~21 requests per league, 6s delay between teams, cache with 24h TTL.

## Goals / Non-Goals

**Goals:**
- Allow users to select which leagues to sync via a UI dialog
- Allow users to select which season to sync via a UI dialog
- Execute sync across multiple selected leagues serially
- Respect the daily API budget (pre-check + graceful stop)
- Expose available leagues and seasons from backend config
- Thread-safe progress tracking with expanded fields

**Non-Goals:**
- No changes to how individual team/player sync works
- No Player entity changes (no season field)
- No parallel league execution
- No UI for viewing historical syncs or per-season data

## Decisions

1. **Serial league execution** — Rate limit (100/day) is the binding constraint. Parallelism burns quota faster with no throughput benefit. The existing 6s team delay and cache TTL make per-league overhead acceptable. Cache hits skip the delay entirely.

2. **One global syncId** — All selected leagues share one syncId. The frontend polls a single endpoint. SyncProgress tracks which league is currently processing via `currentLeagueIndex`.

3. **JSON body POST** — `POST /sync` with `{ leagueIds: [39,140], season: 2024 }` instead of query params. Avoids URL encoding issues with lists. Future-proof for additional params.

4. **GET endpoints for leagues/seasons** — Config-driven. The backend owns the source of truth. No hardcoded ranges in the frontend.

5. **Named executor (pool=1)** — Prevents concurrent syncs. `@Async("footballSyncExecutor")` with single-thread executor ensures FIFO queue. Queue capacity of 10 allows queuing without rejecting.

6. **AtomicInteger for SyncStats** — The polling thread reads while the async thread writes. `AtomicInteger` guarantees visibility without locks.

7. **Rate limit pre-check per league** — Before each league, estimate requests needed. If insufficient budget remains, skip with a warning. Prevents partial league syncs.

8. **No season field on Player** — The app models current roster state, not historical. Adding season would require schema migration, updated unique constraints, and UI changes for unclear value. Future escape hatch: PlayerSeason entity.

## Risks / Trade-offs

- **[Low] Rate limit edge case**: Pre-check estimate may be off if teams have very different squad sizes. → Mitigation: use `teams.size() + 1` as rough estimate per league (1 for teams endpoint + 1 per team for players). The safety margin (10 requests) covers variance.

- **[Medium] Cache serves stale data within 24h**: If a re-sync of the same league+season happens within 24h, the cached API response is returned. → This is correct behavior — the API is the source of truth; re-fetching within 24h would return the same data. The cache TTL matches the API's typical update frequency.

- **[Low] Memory accumulation from progressMap**: Each sync creates a progress entry that lives forever. → Mitigation: scheduled eviction (5 min interval, remove entries older than 30 min).

- **[Low] Overlapping syncs blocked**: If a user requests sync while one is running, it's queued (not rejected). → Acceptable. The queue capacity of 10 is generous for this app.

## Migration Plan

1. **Deploy backend first** (backward compatible):
   - Old `POST /sync?leagueId=140` still works alongside new JSON body
   - `SyncProgress` keeps the old `leagueId` field alongside new list fields
   - New `GET /sync/leagues` and `GET /sync/seasons` endpoints added

2. **Deploy frontend second**:
   - New SyncModal + updated SyncContext send JSON body
   - Old query-param path becomes unused

3. **Cleanup (optional)**:
   - Remove legacy query-param handling
   - Remove old `leagueId` field from SyncProgress

## Open Questions

- Should the league name mapping (39 → "Premier League") be in backend config or fetched from API-Football at runtime? → Proposed: static config map for now (zero API cost). Can be enhanced later.
- Should we expose remaining API quota to the frontend for display? → Proposed: yes, as part of sync progress or a lightweight endpoint.
