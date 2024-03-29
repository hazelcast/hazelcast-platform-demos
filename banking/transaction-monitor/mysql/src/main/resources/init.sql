alter user '@my.other.admin.user@'@'%'
identified with mysql_native_password by '@my.other.admin.password@';

grant all privileges on *.* to '@my.other.admin.user@'@'%';

flush privileges;

use @my.other.admin.database@;

-- Referenced by CommonIdempotentInitialization.java
-- Size limits should match IMapLogger.java
create table mysql_slf4j (
 hash                bigint not null,  
 socket_address      varchar(48) not null, 
 when_ts             timestamp not null,
 level               varchar(8) not null,
 message             varchar(128) not null, 
 thread_name         varchar(48) not null, 
 logger_name         varchar(48) not null, 
 primary key (hash)) 
engine InnoDB;
-- TODO Make primary compound when supported by DataLink
-- primary key (socket_address, when_ts)) 

-- Dummy data, mainly this table will be used for write-through
insert into mysql_slf4j (hash, socket_address, when_ts, level, message, thread_name, logger_name)
values
(0, '127.0.0.1:5701', NOW(), 'INFO', 'Hello World', 'Test', 'Test')
;

select * from mysql_slf4j;
