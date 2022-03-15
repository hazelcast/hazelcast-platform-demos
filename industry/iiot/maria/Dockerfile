FROM library/mariadb:10.7.1

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV MARIADB_ROOT_USERNAME=$MY_OTHERUSER
ENV MARIADB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MARIADB_DATABASE=$MY_OTHERDATABASE

# Initialization scripts
COPY target/classes/0-database.sql   docker-entrypoint-initdb.d/
