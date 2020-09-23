CREATE KEYSPACE @my.other.admin.database@ WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE  @my.other.admin.database@;

CREATE TABLE cpostcode(
  couter TEXT,
  cinner TEXT,
  PRIMARY KEY (couter)
);

DESCRIBE TABLES;