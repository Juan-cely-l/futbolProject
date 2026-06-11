-- Production indexes (PostgreSQL only — loaded via spring.sql.init.platform=postgresql)
CREATE INDEX IF NOT EXISTS idx_player_team_id ON player (team_id);
CREATE INDEX IF NOT EXISTS idx_team_name_lower ON team (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_player_name_lower ON player (LOWER(name));
