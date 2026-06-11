-- Pre-production indexes (cross-platform: H2 + PostgreSQL)
CREATE INDEX IF NOT EXISTS idx_player_team_id ON player (team_id);
