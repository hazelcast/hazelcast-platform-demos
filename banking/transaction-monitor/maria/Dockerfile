FROM --platform=linux/amd64 library/mariadb:10.11.3

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV MARIADB_ROOT_USERNAME=$MY_OTHERUSER
ENV MARIADB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MARIADB_DATABASE=$MY_OTHERDATABASE

# Initialization scripts, applied in collating sequence
COPY target/classes/*.sql       docker-entrypoint-initdb.d/
