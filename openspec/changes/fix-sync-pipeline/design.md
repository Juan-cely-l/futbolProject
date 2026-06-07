## Context

The sync pipeline uses API-Football v3 to fetch teams and players. Current issues:
- `RequestCounter` hardcodes `DAILY_LIMIT = 100` with `SAFETY_MARGIN = 10`, throwing at 90 calls
- `/players` endpoint is called without `&league=` param, returning cross-competition players (50-60 per team)
- `processTeam()` wraps everything in a single transaction: rate limit exception rolls back team creation + all player saves
- Empty cache results are never cached, making recovery impossible
- Team budget `setBudget()` relies on JPA dirty checking without explicit `save()`

## Goals / Non-Goals

**Goals:**
- All teams complete sync without rate limit interrupts mid-transaction
- Squad sizes reflect league-only players (25-35 expected, not 50-60)
- Team creation survives player fetch failures
- Rate limit is configurable from application properties
- Budget is explicitly persisted after recalculation

**Non-Goals:**
- Changing the API-Football provider or client library
- Adding retry logic for rate-limited requests
- Changing the cache TTL or eviction strategy
- Frontend changes

## Decisions

**Decision 1: Add league param to player endpoint**

Add `&league={leagueId}` to the `/players` URI in `ApiFootballClient.getPlayersByTeam()`.

- The league ID is already available in `processTeam()` via the `leagueId` parameter
- Pass it through `getPlayersByTeam(int teamId, int season, int leagueId)` or add a new overload
- This reduces squad size from 50-60 to 25-35

**Decision 2: Two-phase transaction**

Split `processTeam()` into two transaction boundaries:
1. **Team transaction**: Find or create the team entity (no rollback risk for players)
2. **Player transaction**: Fetch and save players, recalculate budget

Implementation: Move player fetch + save logic into a separate `@Transactional` method or use a `TransactionCallback` that only wraps player operations.

**Decision 3: Configurable rate limit**

- Read `football.api.daily-limit` from `application.properties` via `FootballApiConfig`
- `RequestCounter` constructor receives the limit as a parameter
- Keep safety margin as a constant (10) but apply it to the configurable limit

**Decision 4: Explicit budget save**

After `team.setBudget(budget)`, call `teamRepository.save(team)` to ensure the budget is persisted even if the entity becomes detached.

## Risks / Trade-offs

- [Risk] Adding league filter changes data semantics: player stats will no longer include cup/European performances
  → Mitigation: This is the expected behavior (we're syncing league squads, not all-competition history). Match data is scoped to the league.
- [Risk] Two-phase transaction could leave teams without budgets if player phase fails
  → Mitigation: Team is created with minimum budget (€5M) in phase 1. Phase 2 recalculates. If phase 2 fails, the team still has the minimum.
- [Risk] Configurable rate limit could be set too high, leading to 429 errors from the API
  → Mitigation: Document the default (100) and instruct users to match their API plan.
