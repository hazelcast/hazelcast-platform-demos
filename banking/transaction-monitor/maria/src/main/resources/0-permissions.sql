CREATE USER '@my.other.admin.user@'@'%' 
IDENTIFIED BY '@my.other.admin.password@';

SELECT User, Host FROM mysql.user;

GRANT ALL PRIVILEGES ON *.* to '@my.other.admin.user@'@'%'
IDENTIFIED BY '@my.other.admin.password@';

FLUSH PRIVILEGES;

SHOW GRANTS FOR '@my.other.admin.user@'@'%';
