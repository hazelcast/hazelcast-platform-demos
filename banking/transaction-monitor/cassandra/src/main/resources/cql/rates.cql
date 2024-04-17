-- Can't use preferred choice "@my.other.admin.database@", Cassandra doesn't permit dash
CREATE KEYSPACE @my.transaction-monitor.flavor@
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
AND durable_writes = true;

USE "@my.transaction-monitor.flavor@";

CREATE TABLE rates(
  id INT,
  start_timestamp TIMESTAMP,
  amount INT,
  created_by TEXT,
  PRIMARY KEY (id)
);

DESCRIBE TABLE rates;

INSERT INTO rates(id, start_timestamp, amount, created_by)
VALUES (0, toTimeStamp(toDate(now())), 0, '@my.other.admin.user@');

SELECT * FROM rates;

