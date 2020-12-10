FROM mysql:8.0.21

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# ENV uses ARG
ENV MYSQL_DATABASE=$MY_OTHERDATABASE
ENV MYSQL_USER=$MY_OTHERUSER
ENV MYSQL_PASSWORD=$MY_OTHERUSER
ENV MYSQL_ROOT_PASSWORD=$MY_OTHERPASSWORD

# Setup
COPY target/classes/init.sql     /docker-entrypoint-initdb.d 
ENTRYPOINT ["/entrypoint.sh"]

CMD ["mysqld", "--log-bin=ON", "--binlog_format=ROW", "--server-id=1"]
