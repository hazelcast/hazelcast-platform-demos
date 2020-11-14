FROM mongo:4.2.0

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV MONGO_INITDB_ROOT_USERNAME=$MY_OTHERUSER
ENV MONGO_INITDB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MONGO_INITDB_DATABASE=$MY_OTHERDATABASE

# "Options set by command line","attr":{"options":{"net":{"bindIp":"*"}}}}
COPY target/classes/mongod.conf      /etc
#COPY target/classes/roles.js /docker-entrypoint-initdb.d/
COPY target/classes/roles.sh /docker-entrypoint-initdb.d/
RUN chmod 755 /docker-entrypoint-initdb.d/roles.sh

# Override /usr/local/bin/docker-
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh
ENTRYPOINT ["/custom-entrypoint.sh", "mongod", "--replSet", "churn"]
