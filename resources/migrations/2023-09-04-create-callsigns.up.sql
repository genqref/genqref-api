CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE callsigns (
  id INTEGER GENERATED ALWAYS AS IDENTITY,
  user_id INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  symbol VARCHAR(128) NOT NULL,
  token VARCHAR(128) NOT NULL DEFAULT digest(gen_random_bytes(1024), 'sha256'),
  PRIMARY KEY(id),
  UNIQUE(user_id, symbol)
  -- CONSTRAINT fk_user
  --     FOREIGN KEY(user_id)
  --         REFERENCES users(id)
);

CREATE INDEX callsigns_token ON callsigns (token);
