## 1. Authentication (Spring Security)

- [x] 1.1 Add `spring-boot-starter-security` dependency to `pom.xml`
- [x] 1.2 Create `SecurityConfig.java` — HTTP Basic auth, GET public, POST/PUT/DELETE authenticated, `ADMIN_PASSWORD` from env, BCrypt

## 2. Rate Limiting

- [x] 2.1 Create `RateLimitFilter.java` with sliding window (100 req/min per IP) using `ConcurrentHashMap`
- [x] 2.2 Register `RateLimitFilter` in Spring configuration (auto via @Component)

## 3. Nginx Security Headers

- [x] 3.1 Add security headers to `frontend/nginx.conf`: CSP, Strict-Transport-Security, X-Frame-Options, X-Content-Type-Options, Referrer-Policy

## 4. Secrets Management

- [x] 4.1 Create `.env.example` with placeholder values for all required env vars
- [x] 4.2 Move `spring.config.import=file:.env` from `application.properties` to `application-dev.properties`
- [x] 4.3 Update `src/test/resources/application.properties` — API key uses env var `FOOTBALL_API_KEY_TEST` with fallback

## 5. Input Validation

- [x] 5.1 Add `@Size(min=2, max=50)` to `CreatePlayerRequest.name`

## 6. Docker & Build Alignment

- [x] 6.1 Update `Dockerfile` — build stage to `maven:3-eclipse-temurin-21`, runtime to `eclipse-temurin:21-jre-alpine`

## 7. Pre-commit Hook

- [x] 7.1 Add `gitleaks` scan to `.githooks/pre-commit`

## 8. Tests

- [x] 8.1 Write unit tests for `RateLimitFilter` (under limit, over limit, different IPs)
- [x] 8.2 Write unit tests for `SecurityConfig` (unauthenticated POST returns 401, unauthenticated GET returns 200)
- [x] 8.3 Run all backend tests and verify 0 failures
