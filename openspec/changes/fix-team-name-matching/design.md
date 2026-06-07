## Context

The external sync creates duplicate teams because seed data stores names with suffixes ("arsenal fc", "chelsea fc") while API-Football returns bare names ("Arsenal", "Chelsea"). `findTeamByNameIgnoreCase("arsenal")` misses "arsenal fc", creating a new team. Additionally, the sync's rate-limit budget is underestimated — `ESTIMATED_REQUESTS_PER_TEAM = 1` doesn't account for paginated player fetches (3+ pages for large squads), causing mid-sync failures that leave matched teams with 0 players.

## Goals / Non-Goals

**Goals:**
- Seed and API team names match so `findTeamByNameIgnoreCase` finds existing teams
- Eliminate duplicate team creation during external sync
- Rate-limit budget accurately estimates total API requests needed
- Remove double `getTeamsByLeague()` API call (called once in estimate, again in process)

**Non-Goals:**
- No schema changes (no `externalId` column, no data migration)
- No new entities or repositories
- No changes to the frontend display logic
- No retroactive cleanup of existing duplicate teams (manual or separate operation)

## Decisions

### 1. Normalize in the mapper — NOT in the seeder

- **Decision**: Add suffix normalization in `FootballDataMapper.toTeamInfo()` — strip common club suffixes (fc, cf, ud, afc, ac) from the API team name before matching
- **Also normalize in DataSeeder**: Strip the same suffixes at seed time so the DB stores bare names
- **Rationale**: The mapper is the single point where API data enters the system, so normalizing there covers all lookup paths. Normalizing in the seeder ensures new seed imports also produce bare names.
- **Alternatives considered**:
  - Add an `externalId` column to Team entity — cleanest but requires schema migration, contradicts non-goals
  - Strip suffixes in `ExternalFootballService` lookup — works but scatters normalization logic
  - Modify seed JSON data directly — tedious, error-prone, and requires manual maintenance

### 2. Dynamic page-count-based rate estimate

- **Decision**: Change `ESTIMATED_REQUESTS_PER_TEAM` to `5` (safe upper bound: 20/page × 100 max players = 5 pages) AND expose the actual page count from the first `getPlayersByTeam` response. Use the actual count for the pre-sync estimate where possible.
- **Rationale**: A fixed constant is simpler but doesn't handle edge cases. A dynamic approach using the first team's page count per league gives accurate estimates. Default to the constant as fallback.
- **Alternatives considered**: Calculating exact per-team pages by fetching first page of each team during estimate phase — too many API calls, defeats the purpose

### 3. Cache team list between estimate and process

- **Decision**: In `estimateRequestsForLeague`, cache the `List<TeamData>` result and pass it to `processLeague` instead of calling `getTeamsByLeague` twice
- **Rationale**: Saves 1 API request per league (5 requests/day). Simple and safe since the team list doesn't change between estimate and process within the same sync.
- **Alternatives considered**: Re-fetching (current behavior) — wastes budget. Caching in a field — works but adds mutable state. Method parameter is the cleanest approach.

## Risks / Trade-offs

- **[False positive matches]** Stripping suffixes could cause collisions (e.g., hypothetical "Real FC" vs API "Real"). Mitigation: suffix stripping is conservative (only common patterns at end of name, case-insensitive). The match is then exact via `findTeamByNameIgnoreCase`, so a collision requires two separate teams with the same bare name — unlikely in practice.
- **[Broken matching for non-English suffixes]** The list of suffixes (fc, cf, ud, afc, ac) covers the seed data but may miss club-specific variants in other leagues. Mitigation: the current scope is Premier League and La Liga; adding more suffixes is a one-line change.
- **[Rate estimate still wrong for extreme edge cases]** If a team has 200+ players (very unlikely for football squads), 5 pages may not suffice. Mitigation: page count exposure from the API client makes the estimate adaptive, and the hard safety check in `RequestCounter` remains as ultimate guard.
