## ADDED Requirements

### Requirement: Sync progress includes per-team results

When a sync operation completes (status is SUCCESS, PARTIAL, or FAILED), the SyncProgress response SHALL include a `teams` field containing the list of teams that were successfully processed during the sync.

- The `teams` field SHALL be `null` while sync status is PROCESSING
- The `teams` field SHALL be a non-null, possibly empty list when sync status is not PROCESSING
- Each entry SHALL contain the team's name, country, whether it was created or updated, timestamps, and the full list of players in that team's squad
- If a league or team processing fails mid-way, only successfully completed teams SHALL appear in the list

#### Scenario: Completed sync returns teams with player squads

- **WHEN** a sync has finished with SUCCESS status
- **THEN** the SyncProgress response SHALL contain a `teams` array with one entry per processed team
- **AND** each team entry SHALL contain `name`, `country`, `created` (boolean), `updated` (boolean), plus player squad data

#### Scenario: In-progress sync returns null teams

- **WHEN** a sync is currently PROCESSING
- **THEN** the SyncProgress response SHALL have `teams` set to `null`

#### Scenario: Zero teams processed returns empty array

- **WHEN** a sync completes but no teams were successfully processed
- **THEN** the SyncProgress response SHALL have `teams` set to `[]` (empty list)

### Requirement: SyncPlayerResult captures full player data

Each SyncTeamResult SHALL contain a `players` list with SyncPlayerResult entries, each containing: name, position, age, photo URL, goals, assists, matches played, and market value.

- Player data SHALL be read from the database after the team's TransactionTemplate completes
- All standard Player fields SHALL be included for full frontend display

#### Scenario: Team with players returns complete squad

- **WHEN** a team was processed and has players in the database
- **THEN** the team's `players` array SHALL contain one entry per player
- **AND** each entry SHALL include all fields: name, position, age, photo, goals, assists, matches, valueMarket

#### Scenario: Team with no players returns empty squad

- **WHEN** a team was created/updated but has no players assigned
- **THEN** the team's `players` array SHALL be `[]` (empty list)

### Requirement: Frontend displays sync results in accordion

The sync modal SHALL display the synced teams and their players in an expandable accordion layout after sync completes.

- Top level SHALL show each team with its name, country, and created/updated badge
- Expanding a team SHALL reveal its full player list
- Each player row SHALL show: position, age, goals, assists, market value
- The accordion SHALL only appear after sync completes (not during processing)

#### Scenario: Successful sync shows team accordion

- **WHEN** sync completes with SUCCESS status and teams data is available
- **THEN** the sync modal SHALL display an accordion listing all synced teams
- **AND** expanding a team SHALL show its player squad with all relevant stats

#### Scenario: Zero teams synced shows empty message

- **WHEN** sync completes but no teams were synced
- **THEN** the sync modal SHALL display "No teams synced" instead of the accordion

#### Scenario: Modal does not show accordion during processing

- **WHEN** sync is in PROCESSING status
- **THEN** the sync modal SHALL continue to show only the progress bar and counters
- **AND** SHALL NOT display the team accordion
