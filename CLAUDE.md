# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Futbix** — Full-stack football (soccer) team/player management app. Spring Boot 4 + React 19 + PostgreSQL, using Docker for local development.

## Architecture

```
futbolProject/
├── src/main/java/futbol/api/com/
│   ├── FutbolApplication.java                  # Spring Boot entry point
│   ├── controllers/                             # REST endpoints
│   │   ├── TeamController.java                  # /futbix/v1/teams
│   │   ├── PlayerController.java                # /futbix/v1/players
│   │   └── seed/SeedController.java             # POST /futbix/v1/seed
│   ├── models/                                  # JPA entities + enums
│   │   ├── Team.java                            # id(UUID), name(unique), budget, city, createdAt(@CreationTimestamp)
│   │   ├── Player.java                          # id(UUID), name, goals, position(enum), age, assists, matches, valueMarket, team(FK LAZY)
│   │   │                                          # unique constraint on (name, age, team_id)
│   │   └── Position.java                        # GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD
│   ├── repositories/                            # Spring Data JPA with custom queries
│   │   ├── TeamRepository.java                  # findTeamByNameIgnoreCase, existsByNameIgnoreCase, findByNameContainingIgnoreCase
│   │   └── PlayerRepository.java                # findPlayersByTeam_Name, findByNameContainingIgnoreCase, sumValueMarketByTeamId (JPQL),
│   │                                            # existsPlayerByNameAndAgeAndTeamName, countByTeam_Id, existsByNameAndAgeAndTeamNameAndIdNot
│   ├── dtos/                                    # Request/Response DTOs with jakarta.validation
│   │   ├── team/                                # CreateTeamRequest, UpdateTeamRequest, TeamResponse (incl. squadCount, createdAt), TeamValueResponse, PlayerEfficiencyResponse
│   │   └── player/                              # CreatePlayerRequest, UpdatePlayerRequest, PlayerResponse
│   ├── services/                                # Interface + Impl per domain
│   │   ├── Team/                                # TeamService, TeamServiceImpl (mapToResponseDto is private)
│   │   └── Player/                              # PlayerService, PlayerServiceImpl, PlayerMapper
│   ├── seed/                                    # Data ingestion system
│   │   ├── DataSeeder.java                      # Reads JSON, upserts teams/players idempotently
│   │   ├── SeedController.java                  # POST endpoint to trigger seeding
│   │   └── dto/                                 # SeedData, LigaData, EquipoData, JugadorStats (uses com.fasterxml.jackson annotations with Jackson 3 via Spring Boot compat)
│   └── exceptions/                              # GlobalExceptionHandler (@RestControllerAdvice, SLF4J logging)
├── src/test/java/futbol/api/com/
│   ├── controllers/                             # @ExtendWith(MockitoExtension.class)
│   ├── services/                                # Mockito + PlayerMapper tests
│   ├── repositories/                            # @SpringBootTest + @Transactional with H2 in-mem
│   └── dtos/                                    # Bean validation tests
├── src/main/resources/
│   ├── application.properties                   # DB connection (no password/username fallbacks — must be set via env)
│   ├── application-dev.properties               # ddl-auto=update, show-sql=true
│   ├── application-prod.properties              # ddl-auto=validate, show-sql=false
│   └── seed-data/liga_stats.json                # 5 leagues, 38 teams, 56 players (2025/26 season)
├── frontend/                                    # React SPA
│   ├── src/
│   │   ├── main.jsx                             # React 19 entry, QueryClient + Router + ToastProvider + ErrorBoundary
│   │   ├── App.jsx                              # Route definitions with useNavigate for Navbar
│   │   ├── App.css                              # Tailwind v4 + custom theme (pitch green, lime accent) + keyframe animations
│   │   ├── api/                                 # Axios instance + entity-specific API modules
│   │   ├── hooks/                               # TanStack React Query v5 hooks
│   │   ├── components/                          # 11 reusable components
│   │   ├── pages/                               # 5 route-level pages + NotFound
│   │   ├── context/                             # ToastContext.jsx (toast notifications via context)
│   │   └── utils/                               # formatCurrency, computeEfficiency, positionColor
│   ├── vite.config.js                           # Vite 7, React plugin, Tailwind v4, /api proxy
│   ├── index.html
│   └── package.json                             # React 19, React Router 7, Tailwind v4, Axios, React Query 5, Recharts
└── src/test/resources/
    └── application.properties                   # H2 in PostgreSQL compatibility mode
```

### Key Patterns

- **Controller → Service Interface → Service Impl** layered architecture
- **DTOs**: Separate request/response DTOs per entity; `PlayerMapper` converts Player entity → PlayerResponse
- **Validation**: `jakarta.validation` annotations on request DTOs, `MethodArgumentNotValidException` → 400 via GlobalExceptionHandler (returns `fieldErrors` array with `field` + `message`)
- **IDs**: UUIDs generated by the database (`GenerationType.UUID`)
- **Exceptions**: `GlobalExceptionHandler` (`@RestControllerAdvice`, SLF4J logging) maps `ResourceNotFoundException` → 404, `ResourceAlreadyExistsException` → 409, `MethodArgumentNotValidException` → 400 (structured), generic `Exception` → 500 (generic message, no details leaked)
- **Pagination**: Both list endpoints accept `page`, `size`, `sortBy`, `sortDir` query params, return `Page<T>`. Default sizes: teams=9, players=15.
- **Search**: Both list endpoints accept optional `search` param for server-side case-insensitive name filtering (`findByNameContainingIgnoreCase`)
- **sortBy validation**: Both services validate `sortBy` against a whitelist of allowed entity field names; invalid values fall back to `"name"`
- **Unique constraints**: `Team.name` has `@Column(unique = true)`. `Player` has `@Table(uniqueConstraints = ...)` on `(name, age, team_id)`. Service layer checks + `DataIntegrityViolationException` catch as safety net (throws `ResourceAlreadyExistsException`)
- **Names normalized**: Team and Player names are lowercased and trimmed on create AND update
- **Lombok entity pattern**: JPA entities use `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` together (all three required — builder needs all-args constructor)
- **TeamResponse** includes `squadCount` (from `PlayerRepository.countByTeam_Id`) and `createdAt` (from `@CreationTimestamp` on `Team`)
- **Seed system**: Jackson 3 (`tools.jackson.databind.ObjectMapper`) deserializes `liga_stats.json`, with `com.fasterxml.jackson.annotation.*` annotations on DTOs (Spring Boot 4 auto-registers `Jackson2AnnotationsModule` for backward compat). Maps Spanish positions (Portero→GOALKEEPER, Delantero→FORWARD, etc.); generates synthetic age/budget/valueMarket per position and league; fully idempotent (upsert by name for teams, name+age+teamName for players)
- **Frontend state**: TanStack React Query v5 for all server state (cache keys `['teams']`, `['players']`, `['squad']`); mutations auto-invalidate on success
- **Frontend routing**: React Router v7 — `/dashboard` (default redirect), `/teams`, `/teams/:name`, `/players`, `/players/:id`
- **API proxy**: Vite dev server proxies `/api/*` → `http://localhost:8080/futbix/v1/*` (no CORS needed in dev)
- **Error Boundary**: `ErrorBoundary` class component wraps `<App />` in `main.jsx`, catches render errors with `componentDidCatch`, shows "Something went wrong" + reload button
- **Docker**: `docker compose up -d` starts all 3 containers; uses `.env` file for credentials; healthchecks on all services

### API Endpoints

| Method | Path | Action |
|--------|------|--------|
| POST | `/futbix/v1/seed` | Seed database from `liga_stats.json` |
| POST | `/futbix/v1/teams` | Create team |
| GET | `/futbix/v1/teams?page=0&size=9&sortBy=name&sortDir=asc&search=` | List all teams (paginated, server-side search) |
| GET | `/futbix/v1/teams/{id}` | Get team by UUID |
| GET | `/futbix/v1/teams/name/{name}` | Get team by name |
| GET | `/futbix/v1/teams/{name}/squad` | Get team squad (players) |
| GET | `/futbix/v1/teams/{name}/value` | Get total squad market value |
| PUT | `/futbix/v1/teams/{id}` | Update team (with duplicate name check) |
| DELETE | `/futbix/v1/teams/{id}` | Delete team (also deletes FK players) |
| POST | `/futbix/v1/players` | Create player |
| GET | `/futbix/v1/players?page=0&size=15&sortBy=name&sortDir=asc&search=` | List all players (paginated, server-side search) |
| GET | `/futbix/v1/players/{id}` | Get player by UUID |
| PUT | `/futbix/v1/players/{id}` | Update player (supports name, position, teamName, and all stat fields; duplicate check before save) |
| DELETE | `/futbix/v1/players/{id}` | Delete player |
| GET | `/futbix/v1/players/efficiency/{id}` | Get player efficiency ((goals+assists)/matches) |

### Test Structure

- **Controller tests**: `@ExtendWith(MockitoExtension.class)`, mock the service, test HTTP status and response body. Use `nullable(String.class)` for optional string params (searches) since `anyString()` doesn't match null.
- **Service tests**: `@ExtendWith(MockitoExtension.class)`, mock repositories, test business logic and error cases
- **Repository tests**: `@SpringBootTest` + `@Transactional`, uses H2 in PostgreSQL compatibility mode, test query methods
- **DTO tests**: Validate `jakarta.validation` constraints with `ValidatorFactory` (must be closed in `@AfterAll static` method)
- **Assertions**: `org.assertj.core.api.Assertions.assertThat` throughout

## Commands

### Docker (full stack)

```bash
# Start all services (PostgreSQL, backend, frontend)
docker compose up -d

# Stack URLs:
#   Frontend: http://localhost:80
#   Backend:  http://localhost:8080/futbix/v1/
#   DB:       jdbc:postgresql://postgres-db:5432/futbol_db (Docker network)
```

### Backend (local dev)

```bash
# Prerequisites: PostgreSQL running, .env file with POSTGRES_* vars

# Start PostgreSQL
docker compose up -d postgres-db

# Run the Spring Boot app (dev profile, port 8080)
POSTGRES_USER=postgres POSTGRES_PASSWORD=futbolroot mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build without tests
mvn clean package -DskipTests

# Run all tests (124 tests)
mvn test

# Run a single test class
mvn test -Dtest=PlayerControllerTest

# Run a specific test method (JUnit 5 format with #, not .)
mvn test -Dtest=PlayerControllerTest#createPlayer_validRequest_returnsCreated

# Populate database with seed data via Docker backend
curl -X POST http://localhost:8080/futbix/v1/seed
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start dev server (port 5173, proxies /api -> backend)
npm run dev

# Production build
npm run build
```

### CI/CD

Branch protection rule required on `main` (GitHub > Settings > Branches):
- Require status checks: **Backend**, **Frontend**, **Docker**
- Dependabot auto-merges only **minor/patch** updates. Major updates are labeled `needs-review` for manual review.
