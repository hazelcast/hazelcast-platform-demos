FROM debezium/connect:1.3

# OpenJDK 11.0.6
RUN java --version

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# ENV uses ARG, see https://github.com/debezium/docker-images/tree/master/connect/1.4
ENV GROUP_ID=churn-kafka-connect
ENV CONFIG_STORAGE_TOPIC=kafka-connect-storage
ENV OFFSET_STORAGE_TOPIC=kafka-connect-offset
ENV LOG_LEVEL=INFO
ENV REST_HOST_NAME=0.0.0.0

# Note, not MY_BOOTSTRAP_SERVERS
ENV BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"
# Force override
ENV MY_MONGO_LOCATION=""
ENV NOTUSED_PLACEHOLDER_USER=$MY_OTHERUSER
ENV NOTUSED_PLACEHOLDER_PASSWORD=$MY_OTHERPASSWORD
ENV NOTUSED_PLACEHOLDER_DATABASE=$MY_OTHERDATABASE

# Specialised start-up script, plus config for Mongo connector
COPY target/classes/mongo.json $KAFKA_HOME/config/
COPY target/classes/custom-entrypoint.sh  /
COPY target/classes/log4j.properties $KAFKA_HOME/config/log4j.properties
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
CMD ["start"]
