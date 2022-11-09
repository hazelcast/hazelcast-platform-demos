CREATE SCHEMA @my.cluster1.name@;
SET search_path TO @my.cluster1.name@;
SHOW search_path;

-- Should correspond to format used by MaxAggregator.java that writes to IMap
-- and to CommonIdempotentInitialization.java that reads from IMap.
CREATE TABLE alerts_log (
 now	                      BIGSERIAL NOT NULL PRIMARY KEY,
 id                           VARCHAR(8) NOT NULL,
 provenance                   VARCHAR(80) NOT NULL,
 whence                       VARCHAR(80) NOT NULL,
 volume                       NUMERIC(17,2) NOT NULL
);

-- Dummy data, to demonstrate loading action of MapStore
INSERT INTO alerts_log (now, id, provenance, whence, volume)
VALUES
 (0, 'DUMMY', '@my.docker.image.prefix@:@project.artifactId@:init.sql', '@maven.build.timestamp@', 0.0)
;

SELECT * FROM alerts_log;
