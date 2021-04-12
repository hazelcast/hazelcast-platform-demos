FROM bitnami/kafka:2.4.0

# OpenJDK 11.0.6
RUN java --version

# Default values provided
ENV MY_ZOOKEEPER "zookeeper:2181"

CMD for ATOPIC in kf_trades ; do kafka-topics.sh --zookeeper $MY_ZOOKEEPER --create --partitions 271 --replication-factor 1 --topic $ATOPIC --config cleanup.policy=compact; done
