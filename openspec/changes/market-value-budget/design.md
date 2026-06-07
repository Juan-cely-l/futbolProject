## Context

The market value calculator (`MarketValueCalculator.java`) currently uses a flawed formula: position base + random noise (0-10M) + goals bonus, multiplied by a symmetric age curve peaking at 26. Team budget is set before players are saved via `ExternalFootballService.buildTeam()` using a flat league multiplier — Man City and Brentford both get the same 50M budget.

Both systems operate independently despite sharing the same data. The squad value query `PlayerRepository.sumValueMarketByTeamId()` already exists but is never called in the sync flow. Player stats (goals, assists, matches) are already fetched from the API and stored in `PlayerInfo` but only goals reach the calculator.

## Goals / Non-Goals

**Goals:**
- Position-aware stat weighting: goals, assists, and matches each contribute according to positional relevance
- Asymmetric age curve: gradual rise to peak (24-28), gentle decline (29-31), steep decline (32+); GK career shifted +2 years
- No random noise: deterministic, reproducible values
- Budget derived from actual squad value after sync completes
- All changes remain within existing data — no additional API calls

**Non-Goals:**
- Adding external data sources (clean sheets, save %, market sentiment)
- Real-time budget updates (calculated once per sync)
- Changing the API response shapes or adding new endpoints

## Decisions

1. **Method signature change**: `calculate(Position, int age, int goals)` → `calculate(Position, int age, int goals, int assists, int matches)`. All four stat fields already exist in `PlayerInfo` — assists and matches were simply not forwarded. No schema change needed.

2. **Piecewise age curve over Gaussian**: A piecewise linear function is simpler to implement, test, and adjust. Each age bracket has a clear formula with documented inflection points. A Gaussian would require tuning mean/sigma and is harder to reason about.

3. **Budget recalc inside existing transaction**: `processTeam()` runs inside `transactionTemplate.executeWithoutResult()`. Since `sumValueMarketByTeamId` reads from the current transaction's uncommitted state, the recalc must happen inside the same transaction — which is already the case when placed after the player upsert loop.

4. **Match ratio factor**: `matches / 38.0` normalizes to a full season, clamped to [0,1]. A `matchesFactor = 0.3 + 0.7 * matchesRatio` prevents players with very few appearances from getting inflated stat bonuses, while rewarding consistent starters.

5. **No fallback budget for non-synced teams**: Teams created via the regular CRUD API (not sync) will keep their current budget. The budget recalculation only applies to teams processed through the external sync pipeline.

## Risks / Trade-offs

- **GK differentiation**: Without clean sheets or save data, GKs differentiate primarily through matches played and age. The low stat weights for GKs (0.5M per goal, 0.3M per assist) mean base value + matches + age curve is the primary signal. Mitigation: accepted limitation — add clean sheet data when the API provides it.
- **Large budgets**: Top teams could get 400M+ budgets. Mitigation: this reflects real-world annual transfer budgets for top PL clubs. No cap needed.
- **DataSeeder inconsistency**: The seeder generates synthetic data and won't have real squad values. Mitigation: the seeder gets an improved (but still synthetic) budget formula, separate from the sync pipeline.
