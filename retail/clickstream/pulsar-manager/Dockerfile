FROM apachepulsar/pulsar-manager:v0.2.0

COPY target/classes/pulsar-manager.properties      /tmp/pulsar-manager.properties

ENV SPRING_CONFIGURATION_FILE=/tmp/pulsar-manager.properties
