FROM bitnami/kafka:2.4.0

# OpenJDK 11.0.6
RUN java --version

# Extra config goes here

CMD kafka-topics.sh --zookeeper zookeeper:2181 --create --partitions 3 --replication-factor 1 --topic trades