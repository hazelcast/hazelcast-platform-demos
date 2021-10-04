CREATE KEYSPACE @my.other.admin.database@ 
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
AND durable_writes = true;

USE @my.other.admin.database@;

CREATE TABLE model(
  id TEXT,
  payload TEXT,
  PRIMARY KEY (id)
) WITH gc_grace_seconds = 5;

DESCRIBE TABLE model;

SELECT * FROM model;
