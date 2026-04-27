-- Align audit_log toward UUID-based spec (F2.1.7) without rewriting V7.
-- We keep BIGSERIAL legacy PK (id) but add a canonical UUID audit_id for external references.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE audit_log
  ADD COLUMN IF NOT EXISTS audit_id UUID DEFAULT gen_random_uuid(),
  ADD COLUMN IF NOT EXISTS user_id UUID;

-- Backfill existing rows with UUIDs where missing.
UPDATE audit_log
  SET audit_id = gen_random_uuid()
  WHERE audit_id IS NULL;

-- Ensure audit_id is unique.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_audit_log_audit_id'
  ) THEN
    ALTER TABLE audit_log
      ADD CONSTRAINT uq_audit_log_audit_id UNIQUE (audit_id);
  END IF;
END
$$;

