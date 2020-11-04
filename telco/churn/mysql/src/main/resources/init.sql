alter user '@my.other.admin.user@'@'%'
identified with mysql_native_password by '@my.other.admin.password@';

grant all privileges on *.* to '@my.other.admin.user@'@'%';

flush privileges;

use @my.other.admin.database@;

create table tariff (  
 id             varchar(24) not null, 
 year           int not null,
 name           varchar(48) not null,
 international  boolean not null,
 rate           double(5, 2) not null,
 primary key (id)) 
engine InnoDB;
