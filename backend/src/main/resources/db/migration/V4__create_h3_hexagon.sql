CREATE TABLE h3_hexagon (
    h3_index VARCHAR(15) PRIMARY KEY,
    boundary GEOMETRY(POLYGON, 4326) NOT NULL,
    resolution INTEGER NOT NULL DEFAULT 9
);
CREATE INDEX idx_h3_boundary_gist ON h3_hexagon USING GIST(boundary);

CREATE TABLE demographics (
    h3_index VARCHAR(15) REFERENCES h3_hexagon(h3_index),
    population_density FLOAT,
    avg_income FLOAT
);