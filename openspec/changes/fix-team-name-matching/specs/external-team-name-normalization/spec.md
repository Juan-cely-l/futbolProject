# external-team-name-normalization Specification

## Purpose
Normalize football club name suffixes in the external API integration layer so team matching works regardless of naming conventions between seed data and API-Football.

## ADDED Requirements

### Requirement: API team names are normalized before matching
The system SHALL normalize API-Football team names by stripping common club suffixes before using them to look up existing teams in the database.

#### Scenario: Team name with FC suffix
- **WHEN** API-Football returns a team name ending with "fc" (e.g., "Arsenal" with seed name "Arsenal FC")
- **AND** the mapper normalizes the name before creating a `TeamInfo` record
- **THEN** the system SHALL strip the suffix and match the remaining bare name against the database

#### Scenario: Team name with CF suffix
- **WHEN** API-Football returns a team name that matches the seed name minus "cf" suffix (e.g., "Villarreal" with seed name "Villarreal CF")
- **THEN** the system SHALL match the team correctly

#### Scenario: Team name with no suffix
- **WHEN** API-Football returns a team name without any suffix (e.g., "Aston Villa")
- **THEN** the system SHALL perform a normal case-insensitive match without stripping anything

#### Scenario: Team name is only a suffix
- **WHEN** API-Football returns a team name that consists entirely of a recognized suffix after stripping
- **THEN** the system SHALL NOT strip it (e.g., "AC Milan" → do not strip "ac" since the remaining " " is empty or single char; stripping only applies when the name has significant content before the suffix)

### Requirement: Seed data stores normalized team names
The system SHALL normalize team names during the seed process, stripping the same club suffixes before persisting to the database.

#### Scenario: Seed imports bare team name
- **WHEN** `DataSeeder.runSeed()` reads a team name like "Arsenal FC"
- **THEN** the team SHALL be stored as "arsenal" (without the "fc" suffix), not "arsenal fc"

#### Scenario: Seed with already bare name
- **WHEN** `DataSeeder.runSeed()` reads a team name without any suffix like "Aston Villa"
- **THEN** the team SHALL be stored as-is (only lowercased and trimmed)

### Requirement: Normalized suffix list SHALL be configurable
The system SHALL maintain a clear list of recognized suffixes used for normalization in a single location.

#### Scenario: Suffix list centralized
- **WHEN** a developer needs to add or remove a normalized suffix
- **THEN** they SHALL only need to modify one constant set in the normalization logic

#### Scenario: Common suffixes covered
- **WHEN** evaluating which suffixes to normalize
- **THEN** the list SHALL include at minimum: "fc", "cf", "ud", "afc", "ac"
