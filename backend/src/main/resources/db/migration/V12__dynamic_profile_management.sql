ALTER TABLE dynamic_profile
    ADD COLUMN name VARCHAR(120),
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN archived_at TIMESTAMP;

UPDATE dynamic_profile
SET
    name = LEFT(NULLIF(BTRIM(user_query), ''), 120),
    updated_at = generated_at
WHERE name IS NULL;

ALTER TABLE dynamic_profile
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX idx_dynamic_profile_user_active_generated
    ON dynamic_profile(user_id, archived_at, generated_at DESC);
