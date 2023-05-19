CREATE SCHEMA @my.cluster1.name@;
SET search_path TO @my.cluster1.name@;
SHOW search_path;

-- Should correspond to format used by MaxVolumeAggregator.java that writes to IMap
-- and to CommonIdempotentInitialization.java that reads from IMap.
CREATE TABLE alerts_max_volume (
 now	                      BIGSERIAL NOT NULL PRIMARY KEY,
 symbol                       VARCHAR(8) NOT NULL,
 provenance                   VARCHAR(80) NOT NULL,
 whence                       VARCHAR(24) NOT NULL,
 volume                       NUMERIC(17,2) NOT NULL
);

-- Dummy data, to demonstrate loading action of MapStore
INSERT INTO alerts_max_volume (now, symbol, provenance, whence, volume)
VALUES
 (0, 'DUMMY', '@my.docker.image.prefix@:testdata', '@maven.build.timestamp@', 0.0)
;

SELECT * FROM alerts_max_volume;


