CREATE OR REPLACE FUNCTION set_symbol_from_payload()
RETURNS TRIGGER AS $$
BEGIN
  NEW.symbol = NEW.payload->>'symbol';
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE ships_projection (
  id BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  symbol VARCHAR(128) NOT NULL,
  payload JSONB NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT upsert_unique UNIQUE (context, symbol)
);

CREATE TRIGGER ships_projection_set_symbol_from_payload
BEFORE INSERT ON ships_projection
FOR EACH ROW
EXECUTE PROCEDURE set_symbol_from_payload();
