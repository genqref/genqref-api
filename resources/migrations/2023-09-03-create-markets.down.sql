-- http://www.proftpd.org/docs/howto/SQL.html

DROP INDEX markets_valid_at;
DROP INDEX markets_waypoint_symbol;
DROP INDEX markets_system_symbol;
DROP INDEX markets_valid_at_waypoint_symbol;
DROP INDEX markets_valid_at_system_symbol;

DROP TABLE markets;
