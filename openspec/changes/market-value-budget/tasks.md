## 1. Rewrite MarketValueCalculator

- [x] 1.1 Add position-based stat weights (goals, assists, matches) as constants
- [x] 1.2 Implement asymmetric piecewise age curve (GK career +2 years)
- [x] 1.3 Implement matches-played consistency factor
- [x] 1.4 Change method signature: `calculate(Position, int age, int goals, int assists, int matches)`
- [x] 1.5 Remove random noise (`ThreadLocalRandom`)

## 2. Update FootballDataMapper

- [x] 2.1 Pass `playerInfo.assists()` and `playerInfo.appearences()` to the calculator in `toPlayerData()`
- [x] 2.2 Remove `toMarketValue(PlayerInfo)` delegation (stats now flow through `toPlayerData`)

## 3. Update DataSeeder

- [x] 3.1 Pass assists and matches to the new `calculate()` method signaturer

## 4. Recalculate budget from squad value in ExternalFootballService

- [x] 4.1 Add `BUDGET_TO_SQUAD_RATIO` map replacing old `BUDGET_MULTIPLIER`
- [x] 4.2 Remove `BASE_BUDGET` constant and old `BUDGET_MULTIPLIER` map
- [x] 4.3 In `processTeam()`, after player upsert loop, call `sumValueMarketByTeamId(team.getId())` and set budget
- [x] 4.4 Modernize `buildTeam()` to not set budget (it will be set after players are processed)

## 5. Update tests

- [x] 5.1 Rewrite `MarketValueCalculatorTest`: exact-value assertions (no range-based), new cases for all positions and stat combos, age curve tests including GK shift
- [x] 5.2 Update `FootballDataMapperTest`: new mock expectations for assists and matches
- [x] 5.3 Update `ExternalFootballServiceTest`: verify budget is calculated from squad value after processing
