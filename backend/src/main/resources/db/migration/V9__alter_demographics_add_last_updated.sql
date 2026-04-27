-- Add last_updated (F2.1.5) without rewriting V4.
ALTER TABLE demographics
  ADD COLUMN IF NOT EXISTS last_updated TIMESTAMP NOT NULL DEFAULT NOW();

