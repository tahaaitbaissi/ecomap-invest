CREATE TABLE hexagon_score (
    h3_index VARCHAR(15) REFERENCES h3_hexagon(h3_index),
    profile_id UUID REFERENCES dynamic_profile(id) ON DELETE CASCADE,
    score FLOAT NOT NULL CHECK(score BETWEEN 0 AND 100),
    last_calculated TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (h3_index, profile_id)
);
CREATE INDEX idx_hexagon_score_profile ON hexagon_score(profile_id);