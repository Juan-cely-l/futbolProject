## ADDED Requirements

### Requirement: Multi-league serial execution
The system SHALL execute sync across multiple selected leagues serially, one league at a time, processing all teams within each league before moving to the next.

#### Scenario: Multiple leagues synced in order
- **WHEN** sync is initiated with `leagueIds: [39, 140]`
- **THEN** the system SHALL process all teams of league 39 first
- **AND** SHALL then process all teams of league 140
- **AND** SHALL report progress including `totalLeagues`, `processedLeagues`, and `currentLeagueIndex`

#### Scenario: Single league sync unchanged
- **WHEN** sync is initiated with a single league ID
- **THEN** the system SHALL behave identically to the current single-league sync

### Requirement: Rate limit pre-check
The system SHALL check the remaining API request budget before processing each league and skip leagues where the remaining budget is insufficient.

#### Scenario: Insufficient budget skips league
- **WHEN** remaining API requests are fewer than the estimated cost for the next league
- **THEN** the system SHALL skip that league and add a warning to the sync errors list
- **AND** SHALL continue to the next league if budget allows

#### Scenario: Budget exhausted mid-sync
- **WHEN** an API rate limit error is received during a league
- **THEN** the system SHALL stop processing remaining leagues
- **AND** SHALL set sync status to PARTIAL with a message explaining which leagues were skipped

### Requirement: Daily budget auto-reset
The system SHALL reset the daily request counter automatically at midnight.

#### Scenario: Counter resets at midnight
- **WHEN** a new calendar day begins
- **THEN** the request counter SHALL reset to zero
- **AND** full budget SHALL be available for the new day

### Requirement: Thread-safe progress tracking
The system SHALL use thread-safe data structures for progress fields that are read by the polling thread and written by the async sync thread.

#### Scenario: Progress reads during active sync
- **WHEN** the polling endpoint reads `SyncProgress` while sync is processing
- **THEN** the returned values SHALL be consistent (no torn reads)
- **AND** SHALL reflect the most recent write

### Requirement: Progress map eviction
The system SHALL periodically remove stale progress entries to prevent memory accumulation.

#### Scenario: Stale entry cleaned up
- **WHEN** a sync has been completed for more than 30 minutes
- **THEN** its progress entry SHALL be removed from the in-memory map

### Requirement: Progress includes league context
The system SHALL report which league is currently being processed as part of sync progress.

#### Scenario: Frontend displays current league
- **WHEN** sync is processing league 2 of 5 (e.g., Serie A)
- **THEN** the frontend SHALL display "League 2 of 5: Serie A" in the progress section
- **AND** SHALL show the team-level progress within that league

### Requirement: Async executor isolation
The system SHALL use a dedicated single-thread executor for sync operations to prevent concurrent syncs.

#### Scenario: Second sync request queued
- **WHEN** a sync is already in progress and another sync request arrives
- **THEN** the second request SHALL be queued (not rejected)
- **AND** SHALL execute after the first completes
