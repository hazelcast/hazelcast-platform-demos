FROM --platform=linux/amd64 library/openjdk:21-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV MY_BOOTSTRAP_SERVERS="0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"
ENV MY_PULSAR_ADDRESS="0.0.0.0"

ENV JAVA_ARGS=""
ENV JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo java --version \
      && java --version \
      && echo @@@@@@@@@@ \
      && echo @@@@@@@@@@ \
      && echo java $JAVA_ARGS $JAVA_OPTS -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS -Dmy.pulsar.address=$MY_PULSAR_ADDRESS -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener -jar application.jar \
      && java $JAVA_ARGS $JAVA_OPTS -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS -Dmy.pulsar.address=$MY_PULSAR_ADDRESS -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener -jar application.jar \
     "]

