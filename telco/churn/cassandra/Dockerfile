# Debezium 1.4 unclear if works with Cassandra 4.0
FROM cassandra:3.11.4

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Maven compiled target, Debezium connector jar
ARG JAR_FILE
COPY target/${JAR_FILE} /debezium-connector-cassandra/debezium-connector-cassandra.jar
COPY target/classes/debezium-connector-cassandra.conf /debezium-connector-cassandra/debezium-connector-cassandra.conf
COPY target/classes/log4j.properties /debezium-connector-cassandra/log4j.properties

# ENV uses ARG
ENV CASSANDRA_USER=$MY_OTHERUSER
ENV CASSANDRA_PASSWORD=$MY_OTHERPASSWORD
ENV CASSANDRA_CLUSTER_NAME=$MY_OTHERDATABASE
ENV MY_BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

# Setup
COPY target/classes/cql  /cql
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh
ENTRYPOINT ["/custom-entrypoint.sh"]
#CMD ["cassandra", "-f"] 
