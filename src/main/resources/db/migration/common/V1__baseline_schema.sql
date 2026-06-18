CREATE TABLE IF NOT EXISTS team (
    id UUID NOT NULL,
    name VARCHAR(255),
    budget BIGINT,
    city VARCHAR(255),
    created_at TIMESTAMP(6),
    country VARCHAR(100),
    CONSTRAINT pk_team PRIMARY KEY (id),
    CONSTRAINT uk_team_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS player (
    id UUID NOT NULL,
    name VARCHAR(255),
    goals INTEGER,
    position VARCHAR(255),
    age INTEGER,
    assists INTEGER,
    matches INTEGER,
    value_market INTEGER,
    team_id UUID NOT NULL,
    photo VARCHAR(255),
    CONSTRAINT pk_player PRIMARY KEY (id),
    CONSTRAINT uk_player_name_age_team UNIQUE (name, age, team_id),
    CONSTRAINT fk_player_team FOREIGN KEY (team_id) REFERENCES team (id)
);
