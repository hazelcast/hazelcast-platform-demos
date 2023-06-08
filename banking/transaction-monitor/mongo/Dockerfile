# Need AVX on x86_64 for Mongo 5 upwards
#FROM --platform=linux/amd64 mongo:6.0.6
FROM --platform=linux/amd64 mongo:4.4.22

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG FLAVOR
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV FLAVOR=${FLAVOR}
ENV MONGO_INITDB_ROOT_USERNAME=$MY_OTHERUSER
ENV MONGO_INITDB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MONGO_INITDB_DATABASE=$MY_OTHERDATABASE

# Config
COPY target/classes/mongod.conf /etc
COPY target/classes/*.sh /docker-entrypoint-initdb.d/
RUN chmod 755 /docker-entrypoint-initdb.d/*.sh

# Helpful directories, scripts use shell which saves history
RUN mkdir /home/mongodb
RUN chmod 777 /home/mongodb
RUN touch /home/mongodb/.dbshell
RUN chmod 777 /home/mongodb/.dbshell
