-- http://www.proftpd.org/docs/howto/SQL.html

-- CREATE OR REPLACE FUNCTION trigger_set_waypointSymbol
-- RETURNS TRIGGER AS $$
-- BEGIN
--   NEW.waypointSymbol = NEW.payload->>symbol;
--   RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE OR REPLACE FUNCTION trigger_set_systemSymbol
-- RETURNS TRIGGER AS $$
-- BEGIN
--   NEW.systemSymbol = substring(NEW.payload->>symbol from 0 for 7);
--   RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;

CREATE TABLE markets (
  id BIGINT GENERATED ALWAYS AS IDENTITY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by VARCHAR(128) NOT NULL,
  valid_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  context VARCHAR(128) NOT NULL,
  -- -- e.g. X1-GD18-44075C
  -- waypoint_symbol VARCHAR(14) NOT NULL,
  -- -- e.g. X1-GD18
  -- system_symbol VARCHAR(7) NOT NULL,
  payload JSONB NOT NULL,
  PRIMARY KEY(id)
);

-- CREATE TRIGGER set_waypointSymbol
-- BEFORE CREATE ON markets
-- FOR EACH ROW
-- EXECUTE PROCEDURE trigger_set_waypointSymbol();
--
-- CREATE TRIGGER set_systemSymbol
-- BEFORE CREATE ON markets
-- FOR EACH ROW
-- EXECUTE PROCEDURE trigger_set_systemSymbol();

CREATE INDEX markets_valid_at ON markets (valid_at);
-- CREATE INDEX markets_waypoint_symbol ON markets (waypoint_symbol);
-- CREATE INDEX markets_system_symbol ON markets (system_symbol);
-- CREATE INDEX markets_valid_at_waypoint_symbol ON markets (valid_at, waypoint_symbol);
-- CREATE INDEX markets_valid_at_system_symbol ON markets (valid_at, system_symbol);
