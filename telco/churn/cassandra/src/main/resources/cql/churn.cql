CREATE KEYSPACE @my.other.admin.database@ 
WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
AND durable_writes = true;

USE  @my.other.admin.database@;

-- Mirror @CreatedBy, @CreatedDate, @LastModifiedBy, @LastModifiedDate annotations

CREATE TABLE cdr(
  id TEXT,
  caller_telno TEXT,
  caller_mast_id TEXT,
  callee_telno TEXT,
  callee_mast_id TEXT,
  start_timestamp BIGINT,
  duration_seconds INT,
  call_successful BOOLEAN,
  created_by TEXT,
  created_date BIGINT,
  last_modified_by TEXT,
  last_modified_date BIGINT,
  PRIMARY KEY (id)
) WITH cdc=true
AND memtable_flush_period_in_ms = 10000;

DESCRIBE TABLE cdr;
