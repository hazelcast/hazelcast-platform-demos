FROM postgres:13.2

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# ENV uses ARG
ENV POSTGRES_DB=$MY_OTHERDATABASE
ENV POSTGRES_USER=$MY_OTHERUSER
ENV POSTGRES_PASSWORD=$MY_OTHERPASSWORD

# Setup
COPY target/classes/init.sql     /docker-entrypoint-initdb.d
