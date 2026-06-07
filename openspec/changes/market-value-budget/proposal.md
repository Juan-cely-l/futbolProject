## Why

The current market value system uses random noise (0-10M), a symmetric age curve, and only considers goals — ignoring assists and matches. This produces unrealistic, non-deterministic values. Team budget is a flat league multiplier (all PL teams get 50M), ignoring actual squad strength. Together these make player valuation and team comparison unreliable.

## What Changes

- **MarketValueCalculator rewritten**: Position-aware stat weights (goals, assists, matches), asymmetric piecewise age curve (GK peak +2 years), no random noise, deterministic output
- **FootballDataMapper updated**: Pass assists and matches to calculator (data already available in PlayerInfo, just not forwarded)
- **ExternalFootballService modified**: Budget recalculated after players are persisted using `sumValueMarketByTeamId(teamId) * leagueRatio`
- **DataSeeder updated**: Pass all three stats to the new calculator method
- **Tests rewritten**: Exact-value assertions (no range-based for random noise), new cases for all positions and stat combinations

## Capabilities

### New Capabilities
- `market-value-calculator`: Position-aware, stat-weighted, deterministic player market valuation with realistic age curve
- `squad-based-budget`: Team budget derived from actual squad market value rather than flat league multiplier

### Modified Capabilities
- *(none — no existing specs are affected)*

## Impact

- **Backend — Modified**: `MarketValueCalculator.java`, `FootballDataMapper.java`, `ExternalFootballService.java`, `DataSeeder.java`
- **Backend — Tests**: `MarketValueCalculatorTest.java`, `FootballDataMapperTest.java`, `ExternalFootballServiceTest.java`
- **No API changes**: Calculator is internal; budget is persisted on Team entity; API response shapes unchanged
