FROM --platform=linux/amd64 bitnami/kafka:3.4.0

# OpenJDK 11.0.6
RUN java --version

# Default values provided
ENV MY_BOOTSTRAP_SERVERS="0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

# Do not add "kf_alerts" so defaults to 1 partition, easier to find data as low write rate
CMD for ATOPIC in kf_transactions ; do kafka-topics.sh --bootstrap-server  $MY_BOOTSTRAP_SERVERS --create --partitions 271 --replication-factor 1 --topic $ATOPIC --config cleanup.policy=compact; done
