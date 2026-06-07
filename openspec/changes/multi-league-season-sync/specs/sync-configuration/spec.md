## ADDED Requirements

### Requirement: Backend exposes available leagues
The system SHALL expose a `GET /futbix/v1/sync/leagues` endpoint that returns the list of leagues the user can sync, sourced from configuration.

#### Scenario: Request league list
- **WHEN** a client sends `GET /futbix/v1/sync/leagues`
- **THEN** the system SHALL return a 200 response with a JSON array of `{ id: Integer, name: String }` objects
- **AND** the list SHALL be derived from `football.api.league-ids` configuration

### Requirement: Backend exposes available seasons
The system SHALL expose a `GET /futbix/v1/sync/seasons` endpoint that returns the supported season range and the current default season.

#### Scenario: Request season range
- **WHEN** a client sends `GET /futbix/v1/sync/seasons`
- **THEN** the system SHALL return a 200 response with `{ minSeason: Integer, maxSeason: Integer, currentSeason: Integer }`
- **AND** the values SHALL be derived from configuration properties

### Requirement: Sync accepts league and season selection
The system SHALL accept a JSON body on `POST /futbix/v1/sync` with optional `leagueIds` and `season` fields.

#### Scenario: Full selection
- **WHEN** a client sends `POST /futbix/v1/sync` with body `{ "leagueIds": [39, 140], "season": 2024 }`
- **THEN** the system SHALL initiate a sync that includes only the specified leagues and season
- **AND** SHALL return a 202 response with `syncId`, `status`, and selected `leagues` and `season`

#### Scenario: Default selection when fields omitted
- **WHEN** a client sends `POST /futbix/v1/sync` with body `{}`
- **THEN** the system SHALL use all configured leagues as the default
- **AND** SHALL use the configured default season

#### Scenario: Invalid season rejected
- **WHEN** a client sends a `season` value outside the configured `[minSeason, maxSeason]` range
- **THEN** the system SHALL return a 400 error

#### Scenario: Concurrent sync rejected
- **WHEN** a client sends `POST /futbix/v1/sync` while another sync is already in progress
- **THEN** the system SHALL return a 409 Conflict response

### Requirement: Frontend sync dialog
The system SHALL provide a modal dialog for selecting leagues and season before starting a sync.

#### Scenario: Modal opens with available options
- **WHEN** user clicks the Sync button in the Navbar
- **THEN** a modal SHALL open with a list of available leagues (from `GET /sync/leagues`) with checkboxes
- **AND** a season dropdown SHALL be populated from `GET /sync/seasons`, defaulting to `currentSeason`

#### Scenario: Start sync from modal
- **WHEN** user selects leagues and a season and clicks "Start Sync"
- **THEN** the system SHALL call `POST /sync` with the selected `leagueIds` and `season`
- **AND** the modal SHALL display progress until completion

#### Scenario: League selection persisted across sessions
- **WHEN** user closes and re-opens the sync modal
- **THEN** the previously selected leagues SHALL be restored from sessionStorage
