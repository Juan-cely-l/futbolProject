# rate-limiting Specification

## Purpose
Add per-IP rate limiting to all REST endpoints to prevent abuse and resource exhaustion.

## ADDED Requirements

### Requirement: API endpoints have per-IP rate limits
The system SHALL limit API requests to 100 per minute per IP address across all endpoints.

#### Scenario: Under rate limit, request succeeds
- **WHEN** a client sends fewer than 100 requests in a 60-second window
- **THEN** the system SHALL process the request normally

#### Scenario: Over rate limit, request is rejected
- **WHEN** a client exceeds 100 requests in a 60-second window
- **THEN** the system SHALL return HTTP 429 Too Many Requests

#### Scenario: Rate limit resets after window
- **WHEN** a client waits 60 seconds after being rate limited
- **THEN** the client SHALL be able to make requests again

### Requirement: Rate limit applies per IP address
The system SHALL track request counts by client IP address.

#### Scenario: Different IPs have independent counters
- **WHEN** two clients with different IPs each make 100 requests
- **THEN** both clients SHALL be able to make requests without hitting the other's limit

#### Scenario: X-Forwarded-For header is respected
- **WHEN** requests arrive through a reverse proxy with the `X-Forwarded-For` header
- **THEN** the system SHALL use the original client IP for rate limiting

### Requirement: Rate limit blocks are logged
The system SHALL log a warning when a client is rate limited.

#### Scenario: Rate limit violation logged
- **WHEN** a client is rate limited
- **THEN** the system SHALL log the client IP and timestamp at WARN level
