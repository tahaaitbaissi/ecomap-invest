CREATE INDEX IF NOT EXISTS idx_poi_location_gist ON poi USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_poi_type_tag ON poi (type_tag);