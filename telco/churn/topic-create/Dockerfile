FROM --platform=linux/amd64 bitnami/kafka:3.4.0

# OpenJDK 11.0.6
RUN java --version

# Default values provided
ENV MY_BOOTSTRAP_SERVERS="0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

# Create topics shared between producer and consumer, so either can start first.
# kafka-connect creates some extras at runtime for it's own use.
# "debezium.churn.cdr", kafka.topic.prefix from debezium-connector-cassandra.conf + keyspace, table
CMD for ATOPIC in calls,3 connect-status,30 debezium-cassandra.churn.cdr,3 debezium-connector-mongodb.churn.customer,3 ; do kafka-topics.sh --bootstrap-server  $MY_BOOTSTRAP_SERVERS --create --partitions `echo $ATOPIC|cut -d, -f2` --replication-factor 1 --topic `echo $ATOPIC|cut -d, -f1`  --config cleanup.policy=compact; done
