FROM mongo:4.4.0

# Add and confirm Java installed
RUN apt update
RUN apt install -y openjdk-11-jdk
RUN java --version

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Maven compiled target, Debezium connector jar
ARG JAR_FILE
COPY target/${JAR_FILE} /debezium-connector-mongodb/debezium-connector-mongodb.jar
COPY target/classes/debezium-connector-mongodb.conf /debezium-connector-mongodb/debezium-connector-mongodb.conf
COPY target/classes/log4j.properties /debezium-connector-mongodb/log4j.properties

# Environment variables for shell user
ENV MONGO_INITDB_ROOT_USERNAME=$MY_OTHERUSER
ENV MONGO_INITDB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MONGO_INITDB_DATABASE=$MY_OTHERDATABASE
ENV MY_BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

# "Options set by command line","attr":{"options":{"net":{"bindIp":"*"}}}}
COPY target/classes/mongod.conf      /etc
#COPY xxx.js /docker-entrypoint-initdb.d/

# Override /usr/local/bin/docker-
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh
ENTRYPOINT ["/custom-entrypoint.sh", "mongod", "--replSet", "churn"]

