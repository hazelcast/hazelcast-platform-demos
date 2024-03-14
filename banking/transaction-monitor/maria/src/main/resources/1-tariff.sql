use @my.other.admin.database@;

create table tariff (
 id                  bigint not null,  
 base                bigint not null,  
 multiplier          bigint not null,  
 primary key (id)) 
engine InnoDB;

insert into tariff (id, base, multiplier)
values
 (0, 1, 5)
,(1, 2, 7)
,(2, 3, 11)
,(3, 4, 15)
;

select * from tariff;
