# security-headers Specification

## Purpose
Add production security headers to nginx configuration to protect against common web attacks.

## ADDED Requirements

### Requirement: Content-Security-Policy header
The system SHALL set a Content-Security-Policy header restricting resource sources to trusted origins.

#### Scenario: CSP restricts script sources
- **WHEN** a browser loads the application
- **THEN** the response SHALL include `Content-Security-Policy` header restricting `script-src` to `'self'`

#### Scenario: CSP allows Google Fonts
- **WHEN** a browser loads the application
- **THEN** the `Content-Security-Policy` header SHALL allow `style-src` from `fonts.googleapis.com` and `font-src` from `fonts.gstatic.com`

### Requirement: Strict-Transport-Security header
The system SHALL set the Strict-Transport-Security header to enforce HTTPS connections.

#### Scenario: HSTS header present
- **WHEN** a browser receives a response
- **THEN** the response SHALL include `Strict-Transport-Security` header with `max-age=31536000; includeSubDomains`

### Requirement: X-Frame-Options header
The system SHALL set the X-Frame-Options header to prevent clickjacking.

#### Scenario: X-Frame-Options set to DENY
- **WHEN** a browser receives a response
- **THEN** the response SHALL include `X-Frame-Options: DENY` header

### Requirement: X-Content-Type-Options header
The system SHALL set the X-Content-Type-Options header to prevent MIME-sniffing.

#### Scenario: X-Content-Type-Options set to nosniff
- **WHEN** a browser receives a response
- **THEN** the response SHALL include `X-Content-Type-Options: nosniff` header

### Requirement: Referrer-Policy header
The system SHALL set the Referrer-Policy header to limit referrer information leakage.

#### Scenario: Referrer-Policy set to strict-origin-when-cross-origin
- **WHEN** a browser receives a response
- **THEN** the response SHALL include `Referrer-Policy: strict-origin-when-cross-origin` header
