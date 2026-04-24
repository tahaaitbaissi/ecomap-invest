CREATE TABLE dynamic_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_query TEXT NOT NULL,
    drivers_config JSONB NOT NULL,
    competitors_config JSONB NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_dynamic_profile_user_id ON dynamic_profile(user_id);