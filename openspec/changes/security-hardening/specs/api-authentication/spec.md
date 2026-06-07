# api-authentication Specification

## Purpose
Add HTTP Basic authentication via Spring Security — read-only endpoints public, write endpoints protected.

## ADDED Requirements

### Requirement: Mutating endpoints require authentication
The system SHALL require HTTP Basic authentication for all POST, PUT, and DELETE operations.

#### Scenario: Unauthenticated POST request returns 401
- **WHEN** a client sends a POST request to `/futbix/v1/teams` without credentials
- **THEN** the system SHALL return HTTP 401 Unauthorized

#### Scenario: Unauthenticated DELETE request returns 401
- **WHEN** a client sends a DELETE request to `/futbix/v1/players/{id}` without credentials
- **THEN** the system SHALL return HTTP 401 Unauthorized

#### Scenario: Authenticated POST request succeeds
- **WHEN** a client sends a POST request with valid Basic auth credentials
- **THEN** the system SHALL process the request normally

#### Scenario: Authenticated DELETE request succeeds
- **WHEN** a client sends a DELETE request with valid Basic auth credentials
- **THEN** the system SHALL process the request normally

### Requirement: GET endpoints are public
The system SHALL allow unauthenticated GET requests to all endpoints.

#### Scenario: Unauthenticated GET returns 200
- **WHEN** a client sends a GET request to `/futbix/v1/teams` without credentials
- **THEN** the system SHALL return HTTP 200 with the response

### Requirement: Admin password is set via environment variable
The system SHALL read the admin password from the `ADMIN_PASSWORD` environment variable. The application SHALL fail to start if the variable is not set.

#### Scenario: ADMIN_PASSWORD not set prevents startup
- **WHEN** the application starts without the `ADMIN_PASSWORD` environment variable
- **THEN** the application SHALL fail to start with a clear error message

#### Scenario: ADMIN_PASSWORD set allows startup
- **WHEN** the application starts with the `ADMIN_PASSWORD` environment variable set
- **THEN** the application SHALL start normally

### Requirement: Password is hashed
The system SHALL store the admin password using BCrypt password encoding.

#### Scenario: Password not stored in plaintext
- **WHEN** the system authenticates a request
- **THEN** the password SHALL be verified using BCrypt comparison
