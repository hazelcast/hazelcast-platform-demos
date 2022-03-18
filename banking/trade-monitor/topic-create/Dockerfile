FROM bitnami/kafka:3.1.0

# OpenJDK 11.0.6
RUN java --version

# Default values provided
ENV MY_BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

CMD for ATOPIC in kf_trades ; do kafka-topics.sh --bootstrap-server  $MY_BOOTSTRAP_SERVERS --create --partitions 271 --replication-factor 1 --topic $ATOPIC --config cleanup.policy=compact; done
