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

# Config and scripts
COPY target/classes/mongod.conf /etc
COPY target/classes/[0-1]*.sh /docker-entrypoint-initdb.d/
RUN chmod 755 /docker-entrypoint-initdb.d/*.sh
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh

# Anything will do for security keyfile between Mongo instances, and we only have one instance
COPY target/classes/security.keyFile /
RUN chmod 400 /security.keyFile
RUN chown mongodb /security.keyFile

# Helpful directories, scripts use shell which saves history
RUN mkdir /home/mongodb
RUN chmod 777 /home/mongodb
RUN touch /home/mongodb/.dbshell
RUN chmod 777 /home/mongodb/.dbshell

ENTRYPOINT ["/custom-entrypoint.sh", "mongod", "--config", "/etc/mongod.conf", "--replSet", "${MONGO_INITDB_DATABASE}"]
