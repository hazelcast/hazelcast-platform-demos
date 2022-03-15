USE @my.other.admin.database@;

CREATE TABLE wear (
    id         CHAR(2),
    whence     VARCHAR(32), 
    amount     DOUBLE,
    CONSTRAINT wear_pk PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET utf8;

DESCRIBE wear;


