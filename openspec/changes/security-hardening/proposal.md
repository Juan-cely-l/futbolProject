## Why

The application has zero authentication, zero rate limiting, and zero security headers. All 17 REST endpoints are publicly accessible — anyone can create, modify, or delete data. The sync endpoint can consume the entire daily API-Football quota with a single request. Sensitive credentials (API key, DB password) are loaded from `.env` into Spring Environment. This change mitigates all findings from the security audit.

## What Changes

- Add `spring-boot-starter-security` with HTTP Basic auth — GET endpoints public, POST/PUT/DELETE authenticated
- CSRF implicitly handled by stateless Basic auth
- Add rate limiting on all API endpoints (100 req/min per IP)
- Add security headers to nginx (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
- Create `.env.example` with placeholders
- Restrict `.env` import to dev profile only; use env vars for production
- Add `@Size(max=50)` to `CreatePlayerRequest.name`
- Align Java versions in Dockerfile (21 consistently)
- Add `gitleaks` to pre-commit hook

## Capabilities

### New Capabilities
- `api-authentication`: HTTP Basic authentication via Spring Security — read-only endpoints public, write endpoints protected
- `rate-limiting`: Per-IP rate limiting (100 req/min) on all REST endpoints
- `security-headers`: Production security headers in nginx (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
- `secrets-management`: Separate dev/prod secret loading patterns, `.env.example` template, API key rotation notice

### Modified Capabilities
*(No existing spec requirements are changed.)*

## Impact

- `pom.xml`: Add `spring-boot-starter-security`, `resilience4j-ratelimiter`
- `src/main/java/futbol/api/com/config/`: New `SecurityConfig.java`, `RateLimitConfig.java`
- `frontend/nginx.conf`: Add security headers
- `src/main/resources/application.properties`: Restrict `.env` import to dev, remove for prod
- `src/test/resources/application.properties`: Add env-var override for test API key
- `.env.example`: New file with placeholders
- `.env` user must rotate the exposed API key
- `src/main/java/futbol/api/com/dtos/player/CreatePlayerRequest.java`: Add `@Size` constraint
- `Dockerfile`: Align Java version to 21
- `.githooks/pre-commit`: Add gitleaks check
