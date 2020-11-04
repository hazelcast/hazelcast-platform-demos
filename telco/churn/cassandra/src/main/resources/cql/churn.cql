CREATE KEYSPACE @my.other.admin.database@ WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE  @my.other.admin.database@;

CREATE TABLE cdr(
  id TEXT,
  caller_telno TEXT,
  caller_mast_id TEXT,
  callee_telno TEXT,
  callee_mast_id TEXT,
  start_timestamp BIGINT,
  duration_seconds INT,
  successful BOOLEAN,
  PRIMARY KEY (id)
) WITH cdc=true;

DESCRIBE TABLES;