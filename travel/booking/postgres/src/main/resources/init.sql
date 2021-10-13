CREATE TABLE hotel (
 hotel_id                     SERIAL,
 name                         VARCHAR(24) NOT NULL,
 location                     VARCHAR(24) NOT NULL,
 stars                        INTEGER NOT NULL,
 rooms                        INTEGER NOT NULL,
 price                        NUMERIC(11,2)
);

-- "external_ref" is set if the room booking is part of a combined hotel/flight pairing
CREATE TABLE booking (
 booking_id                   SERIAL,
 external_ref                 VARCHAR(24),
 start_date                   DATE NOT NULL,
 nights                       INTEGER NOT NULL,
 cancelled                    BOOLEAN NOT NULL
);

-- This table is essentially a materialized view to expedite searching.
-- When you search you are looking for a sequence of nights when the
-- hotel has rooms. We assume searching only for one room.
CREATE TABLE availability (
 availability_id              SERIAL,
 hotel_id                     INTEGER NOT NULL,
 start_night_inclisive        DATE NOT NULL,
 end_night_exclusive          DATE NOT NULL 
);

CREATE TABLE price_override (
 price_id                     SERIAL,
 hotel_id                     INTEGER NOT NULL,
 start_night_inclisive        DATE NOT NULL,
 end_night_exclusive          DATE NOT NULL,
 price                        NUMERIC(11,2)
);

