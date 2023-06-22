FROM --platform=linux/amd64 library/openjdk:17-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Need existing file so Docker copy does not fail if no keystore/truststore found. Put in target by maven-antrun-plugin
COPY target/${JAR_FILE} target/client.keystore* /
COPY target/${JAR_FILE} target/client.truststore* /
RUN rm /${JAR_FILE}

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_DOCKER_ENABLED "true"
ENV MY_KUBERNETES_ENABLED "true"

ENV JAVA_ARGS ""
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo java --version \
      && java --version \
      && echo @@@@@@@@@@ \
      && echo java $JAVA_ARGS $JAVA_OPTS \
 -DHOST_IP=$HOST_IP \
 -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener \
 -jar application.jar \
      && java $JAVA_ARGS $JAVA_OPTS \
 -DHOST_IP=$HOST_IP \
 -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener \
 -jar application.jar \
     "]

