## Context

Futbix is a development-stage football management app with no security controls. A full security audit found 12 issues ranging from CRITICAL (no authentication) to LOW (missing input validation). This design addresses all findings with practical, minimal-overhead mitigations appropriate for a project of this scale.

## Goals / Non-Goals

**Goals:**
- Authenticate all mutating endpoints (POST/PUT/DELETE) via HTTP Basic auth
- Protect read endpoints (GET) as public but rate-limited
- Add rate limiting (100 req/min per IP) to all endpoints
- Add production security headers to nginx
- Separate dev/prod secret loading (`.env` for dev, env vars for production)
- Add input validation for player names
- Normalize Java versions in Dockerfile to 21
- Improve secret scanning in pre-commit hooks

**Non-Goals:**
- No user registration system (single admin credential, set via env)
- No JWT or OAuth2 (overkill for current scope — HTTP Basic suffices)
- No HTTPS certificate management (requires domain + cert provisioning, out of scope)
- No full audit logging system (basic SLF4J logging is sufficient)
- No encrypted secrets storage (env vars are appropriate for current scale)

## Decisions

### 1. HTTP Basic Auth over JWT/OAuth2

- **Decision**: Use Spring Security with HTTP Basic authentication. GET endpoints are public, all mutating endpoints require authentication. Single admin credential configured via environment variable.
- **Rationale**: Simplest security model that covers the threat. No user registration, no token refresh, no session management. The admin password is injected via `ADMIN_PASSWORD` env var — never hardcoded.
- **Alternatives**: JWT — adds complexity (token generation, refresh, expiration) with no benefit for a single-user app. OAuth2 — entirely inappropriate for this scope. API key header — similar complexity to Basic but without browser standard support.

### 2. In-Memory Rate Limiting over Bucket4j

- **Decision**: Use a simple in-memory `Filter` with a `ConcurrentHashMap<InetAddress, AtomicInteger>` and a sliding window. 100 requests per minute per IP.
- **Rationale**: Zero additional dependencies, trivial implementation, sufficient for preventing accidental abuse. Bucket4j adds a dependency for functionality that can be implemented in ~30 lines.
- **Trade-off**: Rate limits reset on app restart. Not distributed. Acceptable for current single-instance deployment.

### 3. Nginx Security Headers over Spring Security Headers

- **Decision**: Add security headers in `nginx.conf` (reverse proxy layer) rather than Spring Security's built-in header writers.
- **Rationale**: Headers at the edge apply to all responses including static assets and error pages served by nginx. Spring Security headers only apply to proxied API responses.
- **Trade-off**: Headers are hardcoded in config, not dynamic. Acceptable.

### 4. Profile-Based `.env` Loading

- **Decision**: `spring.config.import` of `.env` is only active in the `dev` profile. The `prod` profile uses environment variables exclusively.
- **Rationale**: In Docker (prod), env vars are injected by Docker Compose. In local dev, `.env` provides convenience without risking accidental exposure in prod.
- **Implementation**: `application-dev.properties` contains `spring.config.import=optional:file:.env[.properties]`; `application.properties` removes it.

### 5. Java 21 Consistency

- **Decision**: Align all stages to Java 21 (`maven:3-eclipse-temurin-21` for build, `eclipse-temurin:21-jre-alpine` for runtime).
- **Rationale**: `pom.xml` targets Java 21. Running on Java 25/26 could introduce behavioral changes. Using the same version across build/run eliminates that risk.

## Risks / Trade-offs

- **[Default password]** If `ADMIN_PASSWORD` env var is unset, the app could start with a default or fail. **Mitigation**: Spring Boot fails to start if `ADMIN_PASSWORD` is not set (enforced in `SecurityConfig`).
- **[Rate limit false positives]** Legitimate users hitting 100 req/min could be blocked. **Mitigation**: 100 req/min is generous (1.6 req/sec). Log rate limit blocks for monitoring.
- **[Basic Auth over HTTP]** Credentials travel in plaintext. **Mitigation**: This is a dev/pre-prod mitigation. Full HTTPS is a separate concern requiring domain + certs.
- **[No CSRF tokens]** Basic Auth is stateless, so CSRF does not apply by definition. **Risk**: Acceptable — CSRF is a session-based attack; pure HTTP Basic has no session cookies to exploit.
