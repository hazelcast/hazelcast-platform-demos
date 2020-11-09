FROM bitnami/kafka:2.4.0

# OpenJDK 11.0.6
RUN java --version

# Default values provided
ENV MY_ZOOKEEPER "zookeeper:2181"

# "churn.churn.cdr", cluster, keyspace, table
CMD for ATOPIC in calls churn.churn.cdr ; do kafka-topics.sh --zookeeper $MY_ZOOKEEPER --create --partitions 3 --replication-factor 1 --topic $ATOPIC ; done
