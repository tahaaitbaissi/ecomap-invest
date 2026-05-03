-- Investment records (F14); single resource manager in default deployment; JTA XA is optional (see JtaAtomikosConfiguration).
CREATE TABLE investment (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
  currency VARCHAR(3) NOT NULL DEFAULT 'MAD',
  note VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_investment_user ON investment(user_id);
