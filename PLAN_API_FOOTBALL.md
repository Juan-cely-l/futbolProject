# Implementation Plan: API-Football Integration for Futbix

## Overview

Integrate API-Football (RapidAPI) to sync real team rosters into Futbix. The sync runs asynchronously with polling, processes one team per database transaction (isolating failures), and uses name-based upsert (no externalId on entities). A shared `MarketValueCalculator` is extracted for both DataSeeder and the external sync mapper.

---

## Why The 3 Corrections Matter

### Correction 1: Transaction per team (not one giant transaction)

The original plan wrapped the entire sync in `@Transactional`. If league 3's data caused a constraint violation, leagues 1 and 2 would roll back too. By wrapping each team upsert in its own transaction:

- **Failure isolation**: team 17 fails, teams 1-16 are already committed
- **Shorter locks**: each transaction holds DB locks for milliseconds instead of minutes
- **Independent retry**: a failed team doesn't poison the whole batch

### Correction 2: @Async + polling (no Thread.sleep)

The original plan used `Thread.sleep(1500ms)` in a Tomcat thread to respect API rate limits. That blocks a server thread for 2.5 minutes (100 teams x 1.5s). With `@Async`:

- **Zero server threads blocked**: the async task runs on a pool thread, Tomcat threads stay free
- **202 Accepted immediately**: the HTTP response returns in <10ms, not 2.5 minutes
- **User freedom**: navigating away doesn't cancel the sync; polling resumes on return
- **Real rate limiting**: `RequestCounter` (AtomicInteger) enforces the 100/day limit with actual counting, not sleep

### Correction 3: Upsert by name (no externalId)

Adding `externalId` (Integer) to Team and Player would:

- Require a DB migration (new column, index, unique constraint)
- Add `findByExternalId()` queries
- Create a coupling between API-Football's internal IDs and our DB

Instead, use the same upsert strategy DataSeeder already uses: `existsByNameIgnoreCase()` for teams, `existsByNameAndAgeAndTeamName()` for players. If the API changes a player's name or age, the old and new records coexist -- a manually resolvable edge case that avoids permanent schema coupling.

---

## Files: Create / Modify / Do Not Touch

### Files to CREATE (17 total)

| # | File | Purpose |
|---|------|---------|
| 1 | `external/mapper/MarketValueCalculator.java` | Shared @Component for synthetic market value |
| 2 | `external/config/FootballApiConfig.java` | @ConfigurationProperties for API keys, limits |
| 3 | `external/config/AsyncConfig.java` | @EnableAsync, ThreadPoolTaskExecutor bean |
| 4 | `external/FootballDataProvider.java` | Interface: domain methods (getLeagues, getTeamsByLeague, getPlayersByTeam) |
| 5 | `external/client/RequestCounter.java` | Thread-safe AtomicInteger counter for API requests |
| 6 | `external/client/ApiFootballClient.java` | RestClient implementation of FootballDataProvider |
| 7 | `external/dto/ApiLeagueResponse.java` | DTO for GET /leagues response |
| 8 | `external/dto/ApiTeamResponse.java` | DTO for GET /teams response |
| 9 | `external/dto/ApiPlayerResponse.java` | DTO for GET /players/squads response |
| 10 | `external/dto/SyncResult.java` | Record: sync status result for polling cache |
| 11 | `external/exception/ExternalApiException.java` | RuntimeException for API errors + limit exceeded |
| 12 | `external/mapper/FootballDataMapper.java` | @Component: maps API DTOs to entities |
| 13 | `external/ExternalFootballService.java` | @Service @Async: sync orchestration, transaction per team |
| 14 | `controllers/SyncController.java` | POST 202 + GET polling endpoints |
| 15 | `frontend/src/api/external.js` | Axios API functions for sync endpoints |
| 16 | `frontend/src/hooks/useSyncExternal.js` | useMutation + polling useEffect |
| 17 | -- | -- |

### Files to MODIFY (7 total)

| # | File | Change |
|---|------|--------|
| 1 | `FutbolApplication.java` | Add `@EnableAsync` |
| 2 | `seed/DataSeeder.java` | Inject MarketValueCalculator, remove inline generateValueMarket() |
| 3 | `exceptions/GlobalExceptionHandler.java` | Add handler for ExternalApiException (429 / 502) |
| 4 | `application.properties` | Add football.api.* config keys |
| 5 | `.env` | Add `RAPIDAPI_KEY` and `RAPIDAPI_HOST` |
| 6 | `frontend/src/pages/Dashboard.jsx` | Add "Sync External Data" button + sync status |
| 7 | `docker-compose.yml` | Pass `RAPIDAPI_*` env vars to backend service |

### Files that DO NOT change

| File | Why not touched |
|------|-----------------|
| `models/Team.java` | No externalId needed (upsert by name) |
| `models/Player.java` | No externalId needed (upsert by name+age+team) |
| `models/Position.java` | Existing enum is sufficient |
| `repositories/TeamRepository.java` | Query methods already exist for name-based upsert |
| `repositories/PlayerRepository.java` | Query methods already exist for name+age+team upsert |
| `services/Team/` | External sync uses repositories directly, not these services |
| `services/Player/` | Same reason |
| `seed/SeedController.java` | Unrelated; DataSeeder still works independently |
| `frontend/vite.config.js` | Proxy already catches `/api/*` |
| `frontend/src/main.jsx` | No new providers needed |
| `frontend/src/context/ToastContext.jsx` | Existing toast system is sufficient |
| `frontend/src/App.jsx` | No new routes; Dashboard button is enough |

---

## Implementation Steps (12 Phases)

### Phase 1: Shared MarketValueCalculator

**Why first**: DataSeeder must be refactored to use it, and FootballDataMapper needs it. Everything downstream depends on this being extracted first.

**File**: `src/main/java/futbol/api/com/external/mapper/MarketValueCalculator.java`

Extract the `generateValueMarket` method from DataSeeder into a standalone `@Component`:

```java
package futbol.api.com.external.mapper;

import futbol.api.com.models.Position;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MarketValueCalculator {

    public Integer calculateValue(Position position, int age, int goals) {
        long base = switch (position) {
            case GOALKEEPER -> 8_000_000L;
            case DEFENDER  -> 12_000_000L;
            case MIDFIELDER -> 18_000_000L;
            case FORWARD   -> 25_000_000L;
        };
        double ageFactor = 1.0 - (Math.abs(age - 26) * 0.04);
        ageFactor = Math.max(0.3, Math.min(1.0, ageFactor));
        long goalsBonus = (long) goals * 2_000_000L;
        long randomNoise = ThreadLocalRandom.current().nextLong(0, 10_000_000L);
        return (int) ((base + goalsBonus + randomNoise) * ageFactor);
    }
}
```

**Refactor DataSeeder**: Remove the private `generateValueMarket()` method. Inject `MarketValueCalculator` via constructor (Lombok `@RequiredArgsConstructor` picks it up automatically since it's `final`). Replace calls to `generateValueMarket(position, age, goals)` with `marketValueCalculator.calculateValue(position, age, goals)`.

**Acceptance Criteria**:
- [ ] DataSeeder still compiles and passes tests after replacing inline method
- [ ] MarketValueCalculator is a standalone @Component injectable anywhere
- [ ] The formula logic is identical (base values, age factor, goals bonus, random noise)

---

### Phase 2: Configuration

**Why second**: ApiFootballClient and ExternalFootballService need configuration to know the API URL, key, limits.

#### 2a. AsyncConfig

**File**: `src/main/java/futbol/api/com/external/config/AsyncConfig.java`

```java
package futbol.api.com.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("footballSyncExecutor")
    public Executor footballSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("football-sync-");
        executor.initialize();
        return executor;
    }
}
```

- `corePoolSize=1` because the free tier allows only 100 requests/day -- no benefit from parallelism
- Named `"footballSyncExecutor"` so `@Async("footballSyncExecutor")` targets this specific pool

**Also modify `FutbolApplication.java`**: Remove the `@EnableAsync` annotation since it's now on a @Configuration class. (Or keep both -- having it on the @SpringBootApplication is harmless but redundant. I prefer the dedicated @Configuration for clarity.)

Actually, let me reconsider. The user might have strong opinions about where @EnableAsync goes. Let's put it on the @Configuration class exclusively.

**File**: `src/main/java/futbol/api/com/FutbolApplication.java`

No changes needed here for @EnableAsync (it's on AsyncConfig). But verify: the @SpringBootApplication has `@EnableAsync`? Currently there's none. So no removal needed.

#### 2b. FootballApiConfig

**File**: `src/main/java/futbol/api/com/external/config/FootballApiConfig.java`

```java
package futbol.api.com.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "football.api")
public class FootballApiConfig {
    private String key;
    private String host = "api-football-v1.p.rapidapi.com";
    private String baseUrl = "https://api-football-v1.p.rapidapi.com/v3";
    private int maxRequestsPerDay = 100;
    private int maxRequestsPerSync = 80;  // safety margin
    private int season = 2025;
    private List<Integer> defaultLeagueIds = List.of(39, 140, 78, 135, 61);

    // getters + setters (Lombok @Getter @Setter would also work, but I'll keep it consistent with project conventions)
}
```

**Note**: Use manual getters/setters or Lombok -- check project convention. The rest of the project doesn't use Lombok on config classes. Let me check... The project uses `@RequiredArgsConstructor` on services but no `@Getter/@Setter` on entities (they're manual). I'll use manual getters/setters for consistency... Actually, looking at existing DTOs, they use manual getters/setters too. But the CLAUDE.md says entities use @Builder/@Getter/@Setter from Lombok. For a @ConfigurationProperties, I think using Lombok @Getter/@Setter is fine since it's a simple POJO. But to be safe, I'll use manual getters/setters since @ConfigurationProperties needs setters anyway and Lombok can sometimes cause issues with Spring's property binding.

Actually, `@ConfigurationProperties(prefix = ...)` works perfectly with Lombok `@Getter @Setter`. Let me note in the plan that either approach works.

#### 2c. application.properties

**File**: `src/main/resources/application.properties`

Append at the end:

```properties
# API-Football (RapidAPI)
football.api.key=${RAPIDAPI_KEY}
football.api.host=api-football-v1.p.rapidapi.com
football.api.base-url=https://api-football-v1.p.rapidapi.com/v3
football.api.max-requests-per-day=100
football.api.max-requests-per-sync=80
football.api.season=2025
football.api.default-league-ids=39,140,78,135,61
```

#### 2d. .env

**File**: `.env`

Append:

```properties
RAPIDAPI_KEY=your_key_here
RAPIDAPI_HOST=api-football-v1.p.rapidapi.com
```

#### 2e. docker-compose.yml

**File**: `docker-compose.yml`

Add to the `backend` service `environment` block:

```yaml
RAPIDAPI_KEY: ${RAPIDAPI_KEY}
RAPIDAPI_HOST: ${RAPIDAPI_HOST}
```

**Acceptance Criteria**:
- [ ] Application starts with `RAPIDAPI_KEY` env var (fails fast without it due to placeholder resolution)
- [ ] `@EnableAsync` is active -- verified by injecting an Executor and calling it
- [ ] `FootballApiConfig` binds all properties correctly

---

### Phase 3: API DTOs (Deserialization)

**Why third**: The ApiFootballClient needs these to deserialize JSON responses. They live in `external/dto/` and mirror API-Football's JSON structure.

**Files** (3 files):

#### ApiTeamResponse.java

```java
package futbol.api.com.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiTeamResponse {
    private List<TeamData> response;

    // getters + setters

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamData {
        private ApiTeam team;

        public static class ApiTeam {
            private int id;
            private String name;
            private String country;
            // getters + setters
        }
        // getters + setters
    }
}
```

#### ApiPlayerResponse.java

```java
package futbol.api.com.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiPlayerResponse {
    private List<PlayerData> response;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerData {
        private int id;
        private String name;
        private int age;
        private int number;
        private String position;   // "Goalkeeper", "Defender", "Midfielder", "Attacker"
        private String photo;
        // getters + setters
    }
    // getters + setters
}
```

#### SyncResult.java

```java
package futbol.api.com.external.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SyncResult(
    String status,           // "PROCESSING" | "SUCCESS" | "FAILED"
    int teamsCreated,
    int playersCreated,
    int teamsSkipped,
    int playersSkipped,
    List<String> errors,
    LocalDateTime timestamp
) {
    public static SyncResult processing() {
        return new SyncResult("PROCESSING", 0, 0, 0, 0, List.of(), LocalDateTime.now());
    }

    public static SyncResult failed(String error) {
        return new SyncResult("FAILED", 0, 0, 0, 0, List.of(error), LocalDateTime.now());
    }
}
```

**Acceptance Criteria**:
- [ ] All DTOs have `@JsonIgnoreProperties(ignoreUnknown = true)` to tolerate API response evolution
- [ ] Jackson can deserialize real API-Football responses into these DTOs
- [ ] SyncResult uses Java `record` (immutable, built-in toString/equals)

---

### Phase 4: ExternalApiException + GlobalExceptionHandler

**Why fourth**: ApiFootballClient and ExternalFootballService need a dedicated exception type.

**File**: `src/main/java/futbol/api/com/external/exception/ExternalApiException.java`

```java
package futbol.api.com.external.exception;

public class ExternalApiException extends RuntimeException {
    private final int statusCode;
    private final String apiEndpoint;

    public ExternalApiException(String message, int statusCode, String apiEndpoint) {
        super(message);
        this.statusCode = statusCode;
        this.apiEndpoint = apiEndpoint;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.apiEndpoint = null;
    }

    // getters
}
```

**Modify**: `GlobalExceptionHandler.java`

Add handler method:

```java
@ExceptionHandler(ExternalApiException.class)
public ResponseEntity<Map<String, Object>> handleExternalApi(ExternalApiException exception) {
    log.error("External API error: {}", exception.getMessage());
    HttpStatus status = exception.getMessage().contains("rate limit")
            ? HttpStatus.TOO_MANY_REQUESTS    // 429
            : HttpStatus.BAD_GATEWAY;          // 502
    return ResponseEntity.status(status).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", status.value(),
            "error", "External API Error",
            "message", exception.getMessage()
    ));
}
```

**Acceptance Criteria**:
- [ ] `ExternalApiException` is a RuntimeException (unchecked, no throws declarations needed)
- [ ] Rate limit violations return 429 (TOO_MANY_REQUESTS)
- [ ] API connection errors return 502 (BAD_GATEWAY)
- [ ] Error response leaks no sensitive info (API key, internal URLs)

---

### Phase 5: RequestCounter + FootballDataProvider Interface

**Why fifth**: These form the foundation of the client layer.

**File**: `src/main/java/futbol/api/com/external/client/RequestCounter.java`

```java
package futbol.api.com.external.client;

import futbol.api.com.external.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RequestCounter {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final FootballApiConfig config;

    public int increment() {
        int current = counter.incrementAndGet();
        if (current > config.getMaxRequestsPerDay()) {
            throw new ExternalApiException(
                    "API-Football rate limit exceeded: " + current + "/" + config.getMaxRequestsPerDay()
                            + " requests used today",
                    429, "global"
            );
        }
        return current;
    }

    public int getCount() {
        return counter.get();
    }

    public void reset() {
        counter.set(0);
    }
}
```

**File**: `src/main/java/futbol/api/com/external/FootballDataProvider.java`

```java
package futbol.api.com.external;

import java.util.List;

public interface FootballDataProvider {

    List<LeagueInfo> getLeagues();

    List<TeamInfo> getTeamsByLeague(int leagueId, int season);

    List<PlayerInfo> getPlayersByTeam(int teamId);

    record LeagueInfo(int id, String name, String country) {}
    record TeamInfo(int id, String name, String country) {}
    record PlayerInfo(int id, String name, int age, String position) {}
}
```

- Uses Java records for value types (immutable, no boilerplate)
- Method names reflect **domain concepts**, not API endpoints: `getTeamsByLeague()`, not `fetchTeamsFromApi()`
- This allows swapping ApiFootballClient for a mock or a different provider later

**Acceptance Criteria**:
- [ ] RequestCounter.increment() throws ExternalApiException when over limit
- [ ] RequestCounter is thread-safe (AtomicInteger)
- [ ] FootballDataProvider interface has no API-Football-specific imports (pure domain abstraction)

---

### Phase 6: ApiFootballClient Implementation

**Why sixth**: The core HTTP client. Uses Spring Boot 4's `RestClient` (not RestTemplate, which is deprecated).

**File**: `src/main/java/futbol/api/com/external/client/ApiFootballClient.java`

```java
package futbol.api.com.external.client;

import futbol.api.com.external.FootballDataProvider;
import futbol.api.com.external.dto.ApiPlayerResponse;
import futbol.api.com.external.dto.ApiTeamResponse;
import futbol.api.com.external.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiFootballClient implements FootballDataProvider {

    private final FootballApiConfig config;
    private final RequestCounter requestCounter;

    private final RestClient restClient = RestClient.builder()
            .baseUrl(config.getBaseUrl())
            .defaultHeader("x-rapidapi-key", config.getKey())
            .defaultHeader("x-rapidapi-host", config.getHost())
            .build();

    @Override
    public List<TeamInfo> getTeamsByLeague(int leagueId, int season) {
        requestCounter.increment();
        try {
            ApiTeamResponse response = restClient.get()
                    .uri("/teams?league={league}&season={season}", leagueId, season)
                    .retrieve()
                    .body(ApiTeamResponse.class);

            return response != null && response.getResponse() != null
                    ? response.getResponse().stream()
                    .map(td -> new TeamInfo(td.getTeam().getId(), td.getTeam().getName(), td.getTeam().getCountry()))
                    .toList()
                    : Collections.emptyList();
        } catch (Exception e) {
            throw new ExternalApiException(
                    "Failed to fetch teams for league " + leagueId + ": " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<PlayerInfo> getPlayersByTeam(int teamId) {
        requestCounter.increment();
        try {
            ApiPlayerResponse response = restClient.get()
                    .uri("/players/squads?team={team}", teamId)
                    .retrieve()
                    .body(ApiPlayerResponse.class);

            return response != null && response.getResponse() != null
                    ? response.getResponse().stream()
                    .map(pd -> new PlayerInfo(pd.getId(), pd.getName(), pd.getAge(), pd.getPosition()))
                    .toList()
                    : Collections.emptyList();
        } catch (Exception e) {
            throw new ExternalApiException(
                    "Failed to fetch players for team " + teamId + ": " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<LeagueInfo> getLeagues() {
        requestCounter.increment();
        // For now, return hardcoded leagues from config defaultLeagueIds
        // Full league endpoint support can be added later
        return config.getDefaultLeagueIds().stream()
                .map(id -> new LeagueInfo(id, "League-" + id, "Unknown"))
                .toList();
    }
}
```

**Key decisions**:
- `RestClient` is created as a field (immutable after construction) -- Spring Boot 4's recommended HTTP client
- Each public method calls `requestCounter.increment()` first -- if the limit is hit, the exception prevents the HTTP call
- `getLeagues()` returns defaults from config -- avoids an extra API call for data we already know
- `ExternalApiException` wraps both HTTP errors and connection failures
- No `Thread.sleep()` anywhere -- the rate limit is enforced by counting, not pausing

**Acceptance Criteria**:
- [ ] `ApiFootballClient.getTeamsByLeague(39, 2025)` hits `https://api-football-v1.p.rapidapi.com/v3/teams?league=39&season=2025`
- [ ] All requests include `x-rapidapi-key` and `x-rapidapi-host` headers
- [ ] JSON response is correctly deserialized into ApiTeamResponse/ApiPlayerResponse
- [ ] If request counter exceeds limit, ExternalApiException is thrown BEFORE the HTTP call
- [ ] Service implementation methods call `requestCounter.increment()` before any request

---

### Phase 7: FootballDataMapper

**Why seventh**: After the client returns records (TeamInfo, PlayerInfo), the mapper converts them to JPA entities.

**File**: `src/main/java/futbol/api/com/external/mapper/FootballDataMapper.java`

```java
package futbol.api.com.external.mapper;

import futbol.api.com.external.FootballDataProvider.PlayerInfo;
import futbol.api.com.external.FootballDataProvider.TeamInfo;
import futbol.api.com.models.Player;
import futbol.api.com.models.Position;
import futbol.api.com.models.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FootballDataMapper {

    private final MarketValueCalculator valueCalculator;

    public Team toTeam(TeamInfo teamInfo) {
        return Team.builder()
                .name(teamInfo.name().toLowerCase().trim())
                .city(teamInfo.country() != null ? teamInfo.country().toLowerCase().trim() : "unknown")
                .budget(null)  // API doesn't provide budget; DataSeeder sets this
                .build();
    }

    public Player toPlayer(PlayerInfo playerInfo, Team team) {
        Position position = mapPosition(playerInfo.position());
        int age = playerInfo.age();
        // goals=0 because /players/squads doesn't include stats
        Integer value = valueCalculator.calculateValue(position, age, 0);

        return Player.builder()
                .name(playerInfo.name().toLowerCase().trim())
                .position(position)
                .age(age)
                .goals(0)
                .assists(0)
                .matches(0)
                .valueMarket(value)
                .team(team)
                .build();
    }

    private Position mapPosition(String apiPosition) {
        if (apiPosition == null) return Position.MIDFIELDER;
        return switch (apiPosition.toLowerCase()) {
            case "goalkeeper" -> Position.GOALKEEPER;
            case "defender"   -> Position.DEFENDER;
            case "midfielder" -> Position.MIDFIELDER;
            case "attacker", "forward" -> Position.FORWARD;
            default -> {
                log.warn("Unknown API position '{}', mapping to MIDFIELDER", apiPosition);
                yield Position.MIDFIELDER;
            }
        };
    }
}
```

**Why goals=0 for API players**: `/players/squads` returns roster-only data (name, age, position, number). No stats (goals, assists, appearances). The tradeoff is accepted: roster accuracy is valuable even without stats. Market value is still generated synthetically.

**Acceptance Criteria**:
- [ ] "Goalkeeper" maps to GOALKEEPER, "Defender" to DEFENDER, etc.
- [ ] Unknown positions log a warning and fall back to MIDFIELDER
- [ ] Player entity gets synthetic market value from MarketValueCalculator
- [ ] Team name/city are lowercased and trimmed (matching DataSeeder convention)

---

### Phase 8: ExternalFootballService (@Async + Transaction per Team)

**Why eighth**: The core orchestration service. This is where the three architect corrections converge.

**File**: `src/main/java/futbol/api/com/external/ExternalFootballService.java`

```java
package futbol.api.com.external;

import futbol.api.com.external.dto.SyncResult;
import futbol.api.com.external.exception.ExternalApiException;
import futbol.api.com.external.mapper.FootballDataMapper;
import futbol.api.com.models.Player;
import futbol.api.com.models.Team;
import futbol.api.com.repositories.PlayerRepository;
import futbol.api.com.repositories.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalFootballService {

    private final FootballDataProvider dataProvider;
    private final FootballDataMapper dataMapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final FootballApiConfig config;

    // In-memory cache for polling results. Key = syncId, value = SyncResult.
    // Survives as long as the JVM lives. Backend restart = lost cache (acceptable for v1).
    static final ConcurrentHashMap<String, SyncResult> syncCache = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────
    // PUBLIC: Called by SyncController. Returns immediately.
    // ──────────────────────────────────────────────

    @Async("footballSyncExecutor")
    public CompletableFuture<Void> syncAsync(List<Integer> leagueIds, String syncId) {
        syncCache.put(syncId, SyncResult.processing());
        try {
            SyncResult result = doSync(leagueIds);
            syncCache.put(syncId, result);
            log.info("Sync {} completed: {}", syncId, result);
        } catch (Exception e) {
            log.error("Sync {} failed unexpectedly", syncId, e);
            syncCache.put(syncId, SyncResult.failed("Unexpected error: " + e.getMessage()));
        }
        return CompletableFuture.completedFuture(null);
    }

    // ──────────────────────────────────────────────
    // PRIVATE: Orchestrates the full sync
    // ──────────────────────────────────────────────

    private SyncResult doSync(List<Integer> leagueIds) {
        int totalTeamsCreated = 0;
        int totalPlayersCreated = 0;
        int totalTeamsSkipped = 0;
        int totalPlayersSkipped = 0;
        List<String> errors = new ArrayList<>();

        for (int leagueId : leagueIds) {
            log.info("Processing league {}", leagueId);
            try {
                List<FootballDataProvider.TeamInfo> teams = dataProvider.getTeamsByLeague(leagueId, config.getSeason());

                for (FootballDataProvider.TeamInfo teamInfo : teams) {
                    try {
                        // Each team gets its own transaction
                        TeamSyncResult teamResult = syncTeam(teamInfo);
                        totalTeamsCreated += teamResult.created();
                        totalTeamsSkipped += teamResult.skipped();
                        totalPlayersCreated += teamResult.playersCreated();
                        totalPlayersSkipped += teamResult.playersSkipped();
                    } catch (ExternalApiException e) {
                        // Rate limit hit -- stop processing more teams
                        errors.add("Rate limit reached: " + e.getMessage());
                        break;
                    } catch (Exception e) {
                        // Individual team failure -- log and continue
                        log.error("Failed to sync team {}: {}", teamInfo.name(), e.getMessage());
                        errors.add("Team " + teamInfo.name() + ": " + e.getMessage());
                    }
                }
            } catch (ExternalApiException e) {
                errors.add("League " + leagueId + ": " + e.getMessage());
                break;  // Rate limit or connection failure -- stop
            } catch (Exception e) {
                log.error("Failed to fetch teams for league {}", leagueId, e);
                errors.add("League " + leagueId + ": " + e.getMessage());
            }
        }

        String status = errors.isEmpty() ? "SUCCESS" : (totalTeamsCreated > 0 ? "PARTIAL" : "FAILED");
        return new SyncResult(status, totalTeamsCreated, totalPlayersCreated,
                totalTeamsSkipped, totalPlayersSkipped, errors, LocalDateTime.now());
    }

    // ──────────────────────────────────────────────
    // ONE TRANSACTION PER TEAM: upsert team + its players
    // ──────────────────────────────────────────────

    @Transactional
    public TeamSyncResult syncTeam(FootballDataProvider.TeamInfo teamInfo) {
        String teamName = teamInfo.name().toLowerCase().trim();
        int created = 0, skipped = 0, playersCreated = 0, playersSkipped = 0;

        // UPSERT TEAM
        Team team;
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            team = teamRepository.findTeamByNameIgnoreCase(teamName).orElse(null);
            skipped++;
        } else {
            team = dataMapper.toTeam(teamInfo);
            team = teamRepository.save(team);
            created++;
        }

        if (team == null) {
            return new TeamSyncResult(0, 1, 0, 0);
        }

        // UPSERT PLAYERS
        List<FootballDataProvider.PlayerInfo> playerInfos = dataProvider.getPlayersByTeam(teamInfo.id());
        for (FootballDataProvider.PlayerInfo playerInfo : playerInfos) {
            String playerName = playerInfo.name().toLowerCase().trim();
            int age = playerInfo.age();

            if (playerRepository.existsPlayerByNameAndAgeAndTeamName(playerName, age, teamName)) {
                playersSkipped++;
                continue;
            }

            Player player = dataMapper.toPlayer(playerInfo, team);
            playerRepository.save(player);
            playersCreated++;
        }

        return new TeamSyncResult(created, skipped, playersCreated, playersSkipped);
    }

    // ──────────────────────────────────────────────
    // INNER RECORD for team-level sync result
    // ──────────────────────────────────────────────

    public record TeamSyncResult(int created, int skipped, int playersCreated, int playersSkipped) {
    }
}
```

**Why this structure works**:

1. **`@Async` on a void method** -- The caller (SyncController) doesn't need the CompletableFuture. It only needs the syncId to poll. The async method stores results in the cache. `CompletableFuture.completedFuture(null)` satisfies Spring's AOP proxy requirement.

2. **Transaction per team** -- `@Transactional` on `syncTeam()` means each team's upsert commits independently. If team 7 fails, teams 1-6 are persisted. This is the most important architectural correction.

3. **Try-catch per team** -- Inside `doSync`, each team iteration has its own catch block. A single team failure never halts the entire sync. Errors accumulate in the `errors` list.

4. **Rate limit short-circuit** -- When `ExternalApiException` is caught (from RequestCounter), the loop breaks immediately, saving remaining budget for other operations.

**Acceptance Criteria**:
- [ ] `syncTeam()` is `@Transactional`; `doSync()` is NOT transactional
- [ ] If team 5 fails, teams 1-4 are committed to the database
- [ ] Rate limit exception stops processing more teams but returns partial results
- [ ] SyncResult correctly reflects partial vs complete vs failed status

---

### Phase 9: SyncController (POST 202 + GET Polling)

**Why ninth**: The public API surface. After the service is complete, expose it via REST.

**File**: `src/main/java/futbol/api/com/controllers/SyncController.java`

```java
package futbol.api.com.controllers;

import futbol.api.com.external.ExternalFootballService;
import futbol.api.com.external.dto.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/futbix/v1/external")
@RequiredArgsConstructor
public class SyncController {

    private final ExternalFootballService syncService;
    private final FootballApiConfig config;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> triggerSync(
            @RequestBody(required = false) List<Integer> leagueIds) {

        String syncId = UUID.randomUUID().toString();
        List<Integer> ids = (leagueIds != null && !leagueIds.isEmpty())
                ? leagueIds
                : config.getDefaultLeagueIds();

        syncService.syncAsync(ids, syncId);

        return ResponseEntity.accepted(Map.of(
                "syncId", syncId,
                "status", "processing"
        ));
    }

    @GetMapping("/sync/{syncId}")
    public ResponseEntity<SyncResult> getSyncStatus(@PathVariable String syncId) {
        SyncResult result = ExternalFootballService.syncCache.get(syncId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}
```

**Key observations**:

- The cache is accessed via `ExternalFootballService.syncCache` (public static) -- this avoids injecting the cache. An alternative is to make it injectable, but a static ConcurrentHashMap is simple and sufficient for v1.
- POST returns `202 Accepted` with a `syncId`. The body says `"status": "processing"`.
- GET returns `200` with `SyncResult` if the syncId exists, or `404` if not.
- The frontend polls GET every 5 seconds until `status != "PROCESSING"`.

**Acceptance Criteria**:
- [ ] `POST /futbix/v1/external/sync` returns `202 Accepted` within <100ms
- [ ] `POST /futbix/v1/external/sync` with empty body defaults to all 5 leagues from config
- [ ] `POST /futbix/v1/external/sync` with `[39]` syncs only Premier League
- [ ] `GET /futbix/v1/external/sync/{syncId}` returns `200 SyncResult` when complete
- [ ] `GET /futbix/v1/external/sync/non-existent` returns `404`
- [ ] Rate limit error returns `429` from GlobalExceptionHandler

---

### Phase 10: Backend Tests

**Why tenth**: Validate correctness before the frontend consumes the API.

#### Test files to CREATE (5 files):

| File | Type | What it tests |
|------|------|---------------|
| `external/mapper/MarketValueCalculatorTest.java` | Unit | Value formula: base by position, age curve, goals bonus |
| `external/mapper/FootballDataMapperTest.java` | Unit | Position mapping (all 4 + unknown), team/player entity mapping |
| `external/client/RequestCounterTest.java` | Unit | Atomic counting, exception when over limit, thread safety |
| `external/ExternalFootballServiceTest.java` | Unit | Sync orchestration with mocked provider, error isolation |
| `controllers/SyncControllerTest.java` | Unit | 202 response, 404 for unknown syncId, leagueIds passthrough |

#### Testing patterns to follow:

**MarketValueCalculatorTest** -- pure unit, no Spring context:

```java
@ExtendWith(MockitoExtension.class)  // Or just plain JUnit 5 since no mocks needed
class MarketValueCalculatorTest {
    private final MarketValueCalculator calculator = new MarketValueCalculator();

    @Test
    void calculatesHigherValueForYoungerForwards() {
        Integer youngForward = calculator.calculateValue(Position.FORWARD, 22, 15);
        Integer oldForward = calculator.calculateValue(Position.FORWARD, 35, 15);
        assertThat(youngForward).isGreaterThan(oldForward);
    }

    @Test
    void goalkeeperHasLowerBaseThanForward() {
        Integer gk = calculator.calculateValue(Position.GOALKEEPER, 26, 0);
        Integer fw = calculator.calculateValue(Position.FORWARD, 26, 0);
        assertThat(gk).isLessThan(fw);
    }
}
```

**ExternalFootballServiceTest** -- Mockito, mock provider and repos:

```java
@ExtendWith(MockitoExtension.class)
class ExternalFootballServiceTest {
    @Mock private FootballDataProvider dataProvider;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @InjectMocks private ExternalFootballService service;

    @Test
    void teamFailureDoesNotStopEntireSync() {
        // Mock provider: team1 succeeds, team2 throws
        // Verify: team1's data is saved, sync result has error for team2
    }

    @Test
    void rateLimitExceptionStopsProcessingMoreTeams() {
        // Mock provider: first call succeeds, second throws ExternalApiException("rate limit")
        // Verify: only first team processed, error list contains rate limit message
    }
}
```

**Acceptance Criteria**:
- [ ] All 5 test classes compile and pass
- [ ] MarketValueCalculatorTest verifies age curve (peak at 26, decline on both sides)
- [ ] FootballDataMapperTest verifies all 4 position mappings + fallback
- [ ] ExternalFootballServiceTest verifies error isolation (team failure doesn't stop sync)
- [ ] SyncControllerTest verifies 202 Accepted with syncId in response body

---

### Phase 11: Frontend (API Module + Hook + Dashboard Integration)

**Why eleventh**: The backend is complete and tested. Now the UI consumes it.

#### 11a. API module

**File**: `frontend/src/api/external.js`

```javascript
import api from './axiosInstance'

export const triggerExternalSync = (leagueIds) =>
  api.post('/external/sync', leagueIds || undefined).then((r) => r.data)

export const getSyncStatus = (syncId) =>
  api.get(`/external/sync/${syncId}`).then((r) => r.data)
```

- `leagueIds` is sent as the request body (JSON array). If `undefined`, Spring Boot receives `null` and defaults to all leagues.
- The Axios instance already handles the `/api` prefix via Vite proxy.

#### 11b. Hook with polling

**File**: `frontend/src/hooks/useSyncExternal.js`

```javascript
import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { triggerExternalSync, getSyncStatus } from '../api/external'
import { useToast } from '../context/ToastContext'

export function useSyncExternal() {
  const [syncId, setSyncId] = useState(null)
  const [status, setStatus] = useState('idle') // idle | syncing | success | error
  const qc = useQueryClient()
  const addToast = useToast()

  const mutation = useMutation({
    mutationFn: triggerExternalSync,
    onSuccess: (data) => {
      setSyncId(data.syncId)
      setStatus('syncing')
      addToast('Sync started. Results will appear shortly.', 'info')
    },
    onError: (err) => {
      setStatus('error')
      addToast(err.friendlyMessage || 'Failed to start sync', 'error')
    },
  })

  // Polling loop: checks every 5 seconds while sync is in progress
  useEffect(() => {
    if (!syncId || status !== 'syncing') return

    const interval = setInterval(async () => {
      try {
        const result = await getSyncStatus(syncId)
        if (!result) return // not ready yet

        if (result.status !== 'PROCESSING') {
          clearInterval(interval)
          setSyncId(null)

          if (result.status === 'SUCCESS') {
            setStatus('success')
            addToast(
              `Sync complete: ${result.teamsCreated} teams, ${result.playersCreated} players`,
              'success',
            )
          } else {
            setStatus('error')
            const errors = result.errors?.join('; ') || 'Unknown error'
            addToast(`Sync finished with issues: ${errors}`, 'error')
          }

          // Invalidate caches so the dashboard reflects new data
          qc.invalidateQueries({ queryKey: ['teams'] })
          qc.invalidateQueries({ queryKey: ['players'] })
        }
      } catch (e) {
        // Don't stop polling on a single network error
        console.error('Sync polling error:', e)
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [syncId, status, qc, addToast])

  return { ...mutation, status, syncId }
}
```

**Why the polling pattern is correct**:

- `useMutation` handles the POST (fire-and-forget), storing the syncId
- `useEffect` starts polling when syncId is set, stops when status changes from "PROCESSING"
- Error handling is per-poll-cycle (log + continue), not a global failure
- Cache invalidation (`['teams']`, `['players']`) triggers automatic refetch of all pages

#### 11c. Dashboard integration

**File**: `frontend/src/pages/Dashboard.jsx`

Add at the top of the component (after existing imports):

```javascript
import { useSyncExternal } from '../hooks/useSyncExternal'

export default function Dashboard() {
  const { mutate: startSync, status: syncStatus, isPending } = useSyncExternal()
  // ... existing code ...
```

Add a sync button section above the Hero Metrics grid:

```jsx
{/* Sync Control */}
<div style={{ marginBottom: 24, display: 'flex', alignItems: 'center', gap: 16 }}>
  <button
    onClick={() => startSync()}
    disabled={isPending || syncStatus === 'syncing'}
    style={{
      background: isPending || syncStatus === 'syncing' ? '#334155' : '#166534',
      color: '#fff',
      border: 'none',
      borderRadius: 8,
      padding: '10px 24px',
      fontSize: 14,
      fontWeight: 600,
      cursor: isPending ? 'not-allowed' : 'pointer',
      fontFamily: "'Oswald', sans-serif",
      letterSpacing: '0.03em',
    }}
  >
    {syncStatus === 'syncing' ? 'Syncing...' : 'Sync External Data'}
  </button>
  {syncStatus === 'syncing' && (
    <span style={{ color: '#94A3B8', fontSize: 13 }}>
      Fetching real team data from API-Football...
    </span>
  )}
</div>
```

**Acceptance Criteria**:
- [ ] Button says "Sync External Data" when idle, "Syncing..." when in progress
- [ ] Button is disabled during sync (prevents double-submit)
- [ ] Toast shows "Sync started" immediately, "Sync complete" with counts on success
- [ ] Toast shows error message on failure
- [ ] After sync completes, teams/players data refreshes automatically (cache invalidation)
- [ ] Polling stops when sync completes or user navigates away

---

### Phase 12: Docker + Env Verification

**Why last**: Configuration must work end-to-end in Docker.

**Modified files**:

1. **`.env`** -- Add `RAPIDAPI_KEY` and `RAPIDAPI_HOST`
2. **`docker-compose.yml`** -- Pass them to the backend service
3. **`spring.datasource.username/password` fallback check** -- Verify that `RAPIDAPI_KEY` without a value fails fast (Spring's `${RAPIDAPI_KEY}` placeholder resolver throws on missing var)

**Acceptance Criteria**:
- [ ] `docker compose up -d` starts all 3 containers
- [ ] Backend container logs show no errors related to `RAPIDAPI_KEY` binding
- [ ] `POST /futbix/v1/external/sync` returns 202 in Docker
- [ ] Frontend button works in Docker (connects through Nginx proxy)

---

## Testing Strategy

### Unit Tests (Primary Strategy)

| Component | Test type | What to test |
|-----------|-----------|-------------|
| MarketValueCalculator | Pure unit | Position base values, age curve peaks at 26, goals bonus, random noise range |
| FootballDataMapper | Pure unit | Each position string maps correctly, null position falls back, unknown position warns |
| RequestCounter | Unit | Count increments, exception at limit+1, reset works |
| ExternalFootballService | Unit + Mockito | Error isolation (team failure), rate limit short-circuit, partial results, empty response |
| SyncController | Unit + Mockito | 202 with syncId, 404 for unknown, leagueIds passthrough, null body defaults |

### No Integration/Contract Tests (for now)

- **API-Football has a 100/day free tier** -- hitting it in CI would exhaust the budget
- If WireMock is added later, contract tests with JSON fixtures become valuable
- For now, the ApiFootballClient is thin enough (delegates to RestClient) that Mockito suffices

### No RateLimiter Interface (Not Needed)

The user's original plan mentioned a `RateLimiter` interface for testing. After reviewing:

- `RequestCounter` is already testable: inject a config with `maxRequestsPerDay=3`, call `increment()` 4 times, verify exception
- An interface + NoOp implementation adds indirection without benefit
- The current design is simpler and equally testable

### Test File List (5 new files)

```text
src/test/java/futbol/api/com/external/mapper/MarketValueCalculatorTest.java
src/test/java/futbol/api/com/external/mapper/FootballDataMapperTest.java
src/test/java/futbol/api/com/external/client/RequestCounterTest.java
src/test/java/futbol/api/com/external/ExternalFootballServiceTest.java
src/test/java/futbol/api/com/controllers/SyncControllerTest.java
```

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| API-Football changes response format | Deserialization failure → sync fails with error | `@JsonIgnoreProperties(ignoreUnknown = true)` on all DTOs tolerates new fields. Logged errors make debugging easy. |
| Rate limit exceeded mid-sync | Partial data in DB | RequestCounter enforces limit BEFORE each request. `doSync()` catches `ExternalApiException` and stops gracefully. SyncResult reports partial status with error details. |
| Backend restart during sync | In-progress sync is lost (in-memory cache cleared) | SyncResult is in a ConcurrentHashMap (heap-only). Acceptable for v1. User re-runs the sync. A future version could persist to DB. |
| API-Football is down | Sync fails, no data imported | `ApiFootballClient` wraps connection errors in `ExternalApiException`. User sees toast "Failed to start sync". No impact on existing data. |
| Two syncs started simultaneously | Race condition in RequestCounter | Thread pool size=1 ensures only one sync runs at a time. The second POST returns 202 but is queued. Adding guard code to reject concurrent syncs is a future improvement. |

---

## Success Criteria

- [ ] `MarketValueCalculator` is extracted as a `@Component` and used by both `DataSeeder` and `FootballDataMapper`
- [ ] `POST /futbix/v1/external/sync` returns `202 Accepted` with `syncId` in <100ms (no blocking)
- [ ] `GET /futbix/v1/external/sync/{syncId}` returns `SyncResult` with status `SUCCESS`, `PARTIAL`, or `FAILED`
- [ ] Sync processes one team per database transaction (team A failure does not roll back team B)
- [ ] Rate limit of 100 requests/day is enforced before HTTP calls, not via Thread.sleep
- [ ] No `externalId` column added to Team or Player entities
- [ ] Frontend button triggers sync, polls every 5 seconds, invalidates caches on completion
- [ ] All 5 new test classes pass with `mvn test`
- [ ] DataSeeder still works after refactoring (existing tests pass)
- [ ] `docker compose up -d` starts with the new env vars
