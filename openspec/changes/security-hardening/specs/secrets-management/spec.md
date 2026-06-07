# secrets-management Specification

## Purpose
Separate dev/prod secret loading patterns, provide `.env.example` template, and fix test configuration.

## ADDED Requirements

### Requirement: Dev secrets loaded from .env, prod from environment variables
The system SHALL load `.env` file only when the `dev` Spring profile is active. The `prod` profile SHALL rely exclusively on environment variables.

#### Scenario: Dev profile loads .env
- **WHEN** the application starts with the `dev` profile active
- **THEN** the system SHALL load secrets from the `.env` file in the project root

#### Scenario: Prod profile does not load .env
- **WHEN** the application starts with the `prod` profile active
- **THEN** the system SHALL NOT load the `.env` file
- **AND** all secrets SHALL come from environment variables only

#### Scenario: Missing ADMIN_PASSWORD prevents startup
- **WHEN** the `ADMIN_PASSWORD` environment variable is not set
- **THEN** the application SHALL fail to start with an informative error

### Requirement: .env.example template exists
The system SHALL provide a `.env.example` file with placeholder values for all required environment variables.

#### Scenario: .env.example contains all required vars
- **WHEN** a developer opens `.env.example`
- **THEN** it SHALL list: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `FOOTBALL_API_KEY`, `ADMIN_PASSWORD`

#### Scenario: .env.example uses placeholder values
- **WHEN** a developer opens `.env.example`
- **THEN** all values SHALL be clearly marked as placeholders (e.g., `your_password_here`)

### Requirement: Test API key uses environment variable override
The system SHALL use an environment variable override for the test API key, with a secure default placeholder.

#### Scenario: Test key overridable by env var
- **WHEN** running tests
- **THEN** the API key SHALL be read from `FOOTBALL_API_KEY_TEST` environment variable
- **AND** fall back to a placeholder value if not set
