-- Users table (auth) must be UUID-based.
-- pgcrypto provides gen_random_uuid() on Postgres/PostGIS images.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_INVESTOR',
    company_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);