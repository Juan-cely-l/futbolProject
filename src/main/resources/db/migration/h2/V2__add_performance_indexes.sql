CREATE INDEX IF NOT EXISTS idx_player_team_id ON player (team_id);
CREATE INDEX IF NOT EXISTS idx_team_name ON team (name);
CREATE INDEX IF NOT EXISTS idx_player_name ON player (name);
