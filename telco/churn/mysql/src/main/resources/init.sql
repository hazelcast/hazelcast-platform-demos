alter user '@my.other.admin.user@'@'%'
identified by '@my.other.admin.password@';

grant all privileges on *.* to '@my.other.admin.user@'@'%';

flush privileges;

use @my.other.admin.database@;

create table neil (  
 one  varchar(4) not null, 
 two  varchar(4) not null,
 primary key (one)) 
engine InnoDB;
