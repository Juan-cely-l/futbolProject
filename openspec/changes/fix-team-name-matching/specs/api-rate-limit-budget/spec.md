# api-rate-limit-budget Specification

## Purpose
Accurately estimate API request budget per team and per league to prevent mid-sync failures caused by the daily rate limit being exhausted during player data fetches.

## ADDED Requirements

### Requirement: Rate limit estimate accounts for paginated player fetches
The system SHALL estimate the number of API requests needed per team based on the actual page count of player results, not a fixed constant.

#### Scenario: Estimate uses page count from first player fetch
- **WHEN** the sync estimates requests for a league
- **THEN** the system SHALL account for paginated player fetches by using a safe per-team multiplier (at minimum 5 pages) or by deriving the actual page count from the API response

#### Scenario: Estimate covers all teams in league
- **WHEN** calculating `estimateRequestsForLeague()`
- **THEN** the formula SHALL include: `1 (league teams fetch) + N_teams × pages_per_team`
- **WHERE** `pages_per_team` is at minimum 5 (covering ~100 players at 20/page)

#### Scenario: Fallback when estimation fails
- **WHEN** the team list fetch fails during estimation
- **THEN** the system SHALL use a safe default estimate (at minimum 20 requests per league)

### Requirement: League team list is cached between estimate and process
The system SHALL reuse the team list fetched during the estimate phase rather than fetching it again during processing.

#### Scenario: Single team list fetch per league
- **WHEN** `estimateRequestsForLeague()` retrieves the team list from the API
- **THEN** the same list SHALL be passed to `processLeague()` without a second API call

#### Scenario: Cached list survives per-team processing
- **WHEN** processing individual teams within a league
- **THEN** the cached team list SHALL remain available for the duration of league processing
