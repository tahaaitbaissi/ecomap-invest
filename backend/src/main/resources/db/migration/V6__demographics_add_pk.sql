-- JPA @Id on demographics.h3_index requires a primary key in PostgreSQL.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'demographics_pkey'
  ) THEN
    ALTER TABLE demographics ADD PRIMARY KEY (h3_index);
  END IF;
END
$$;
