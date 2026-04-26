CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_email VARCHAR(255),
    action VARCHAR(64),
    method VARCHAR(255),
    args_summary TEXT,
    duration_ms BIGINT,
    success BOOLEAN,
    error_class VARCHAR(255),
    error_message TEXT
);
CREATE INDEX idx_audit_log_occurred_at ON audit_log(occurred_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);

