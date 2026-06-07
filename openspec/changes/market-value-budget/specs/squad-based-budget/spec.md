## ADDED Requirements

### Requirement: Team budget is derived from squad market value

After a sync operation processes a team's players, the system SHALL recalculate the team's budget based on the total market value of its squad.

- Budget SHALL be calculated as `squadValue * leagueRatio`
- League ratios SHALL reflect real-world revenue differences between leagues
- The recalculation SHALL happen inside the same transaction as player upsert

#### Scenario: Premier League team gets 50% of squad value

- **WHEN** a Premier League team (leagueId 39) completes sync
- **THEN** its budget SHALL be approximately 50% of its total squad market value

#### Scenario: Ligue 1 team gets 30% of squad value

- **WHEN** a Ligue 1 team (leagueId 61) completes sync
- **THEN** its budget SHALL be approximately 30% of its total squad market value

#### Scenario: Very small squad gets minimum budget

- **WHEN** a team's recalculated budget would be below the minimum threshold
- **THEN** the budget SHALL be set to the minimum (5M)

### Requirement: Budget uses existing infrastructure

The system SHALL use `PlayerRepository.sumValueMarketByTeamId()` for the squad value query.

- No new database queries SHALL be added
- The query SHALL run inside the existing sync transaction

#### Scenario: Budget query uses existing repository method

- **WHEN** recalculating team budget
- **THEN** the query SHALL be `playerRepository.sumValueMarketByTeamId(team.getId())`
