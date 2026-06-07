# Sync Pipeline Resilience

## ADDED Requirements

### Requirement: Player fetch filters by league competition

The system SHALL pass the league ID when fetching players from the external API to limit results to the specific competition being synced.

**Rationale:** Without the league filter, `/players` returns players across all competitions (league, domestic cups, European competitions), inflating squad sizes to 50-60 instead of the expected 25-35.

#### Scenario: Premier League sync returns only PL squad
- **WHEN** syncing Premier League (league ID 39)
- **THEN** GET `/players?team={id}&season={year}&league=39` is called
- **THEN** each team has at most 35 players returned

### Requirement: Team creation survives player fetch failure

Team persistence SHALL not be rolled back when player fetching fails due to rate limiting or API errors.

**Rationale:** Currently both team and player operations are in a single transaction; a rate limit exception during player fetch undoes team creation, leaving the team unreachable.

#### Scenario: Rate limit hit during player fetch
- **WHEN** the rate limit is reached while fetching players for a team
- **THEN** the team entity remains persisted in the database
- **THEN** the budget is set to the minimum (€5M)
- **THEN** the sync continues with the next team
- **THEN** the error is recorded in sync errors

### Requirement: Daily rate limit is configurable

The daily API call limit SHALL be read from application properties, not hardcoded.

**Rationale:** The hardcoded limit of 100 (with 10 safety margin = 90) doesn't match the actual API plan, causing premature sync failures.

#### Scenario: Limit can be overridden in properties
- **WHEN** `football.api.daily-limit` is set to 200 in application properties
- **THEN** the system allows up to 200 daily API calls before rate-limiting

### Requirement: Sync estimate accounts for league context

The request estimate for fetching players SHALL account for the league parameter, since filtered results return fewer pages.

#### Scenario: Estimate reflects single page per team
- **WHEN** estimating requests for a league sync
- **THEN** each team is estimated at 1 page rather than 2+
- **THEN** the rate limit pre-check correctly allows full league syncs

### Requirement: Budget persists correctly

The team budget SHALL persist when recalculated from squad value after player processing.

**Rationale:** Currently `team.setBudget()` relies on JPA dirty checking within a transaction; if the transaction rolls back or the entity is detached, the budget update is lost.

#### Scenario: Budget is persisted after player sync
- **WHEN** squad value is calculated from saved players
- **THEN** the updated budget is saved to the database
- **THEN** the team shows the correct budget on subsequent reads
