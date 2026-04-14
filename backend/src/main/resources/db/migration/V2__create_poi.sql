CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE poi (
    id UUID PRIMARY KEY,
    osm_id VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    address VARCHAR(255),
    type_tag VARCHAR(255) NOT NULL,
    location geometry(Point,4326) NOT NULL
);