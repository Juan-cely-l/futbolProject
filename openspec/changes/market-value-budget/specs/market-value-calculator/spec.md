## ADDED Requirements

### Requirement: Market value is calculated from position-aware stats

The system SHALL calculate a player's market value using a deterministic formula that accounts for position, age, goals, assists, and matches played.

- The formula SHALL be deterministic (no random component)
- Different positions SHALL weight stats differently

#### Scenario: Forward market value is dominated by goals

- **WHEN** calculating value for a FORWARD with high goals, moderate assists
- **THEN** goals contribute significantly more than assists to the stat bonus

#### Scenario: Defender goals are heavily weighted

- **WHEN** calculating value for a DEFENDER with 5 goals
- **THEN** each goal has a higher weight than for a FORWARD

#### Scenario: Two identical players produce identical values

- **WHEN** two players have the same position, age, goals, assists, matches
- **THEN** their market values SHALL be identical

### Requirement: Age curve is asymmetric and position-aware

The system SHALL apply an age factor that varies by position and follows a realistic career curve.

- Players aged 24-28 SHALL receive the maximum age factor (1.0)
- Players aged 17-19 SHALL receive lower factors (0.50-0.73) reflecting potential
- Players aged 32+ SHALL receive declining factors reflecting age-related decline
- Goalkeepers SHALL have their effective age shifted by +2 years (GK peaks at 26-30)

#### Scenario: Forward at age 26 gets peak factor

- **WHEN** calculating value for a FORWARD aged 26
- **THEN** the age factor SHALL be 1.0

#### Scenario: Forward at age 32 gets lower factor than age 26

- **WHEN** calculating value for a FORWARD aged 32
- **THEN** the age factor SHALL be lower than for a FORWARD aged 26

#### Scenario: Goalkeeper at age 30 gets peak factor (shifted peak)

- **WHEN** calculating value for a GOALKEEPER aged 30
- **THEN** the age factor SHALL be 1.0 (peak, since GK career is +2 years)

#### Scenario: Near-retirement player gets minimal factor

- **WHEN** calculating value for a player aged 38+
- **THEN** the age factor SHALL be at or near the minimum floor (0.08)

### Requirement: Matches played acts as a consistency multiplier

The system SHALL apply a matches-played factor to prevent players with few appearances from receiving inflated stat bonuses.

- The matches factor SHALL scale from 0.3 (0 matches) to 1.0 (38+ matches)
- The scaling SHALL be linear relative to matches / 38.0

#### Scenario: Player with full season gets full matches factor

- **WHEN** a player has 38+ matches
- **THEN** the matches factor SHALL be 1.0

#### Scenario: Player with few matches gets reduced factor

- **WHEN** a player has fewer than 10 matches
- **THEN** the matches factor SHALL be below 0.5
