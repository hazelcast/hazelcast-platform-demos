use @my.other.admin.database@;

create table users (
 id                  bigint not null,  
 first_name          varchar(48) not null, 
 last_name           varchar(48) not null, 
 primary key (id)) 
engine InnoDB;

-- Dummy data, mainly this table will be used for write-through
insert into users (id, first_name, last_name)
values
 (0, 'John', 'Doe')
,(1, 'Jane', 'Doe')
;

select * from users;
