-- Add missing POI enrichment fields (F2.1.3) without rewriting V2.
-- Existing installs already have the base poi table (V2) + indexes (repeatable R__poi_indexes.sql).

-- Ensure UUID default function is available if we set defaults.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE poi
  ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP NOT NULL DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS price_level INTEGER,
  ADD COLUMN IF NOT EXISTS rating FLOAT;

-- Add constraints (idempotent via DO blocks).
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_poi_price_level'
  ) THEN
    ALTER TABLE poi
      ADD CONSTRAINT chk_poi_price_level CHECK (price_level BETWEEN 1 AND 4);
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_poi_rating'
  ) THEN
    ALTER TABLE poi
      ADD CONSTRAINT chk_poi_rating CHECK (rating BETWEEN 0 AND 5);
  END IF;
END
$$;

-- Align with spec: default UUID value for poi.id.
ALTER TABLE poi
  ALTER COLUMN id SET DEFAULT gen_random_uuid();

