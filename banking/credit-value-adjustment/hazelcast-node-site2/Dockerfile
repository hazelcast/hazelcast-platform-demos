FROM amd64/ubuntu:20.04

# Add and confirm Java installed
RUN apt update
RUN apt install -y openjdk-11-jdk
RUN java --version

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV MY_GRAFANA_SERVICE "grafana-service"
ENV MY_KUBERNETES_ENABLED "true"

ENV JAVA_ARGS ""
ENV JAVA_HEAP_SIZE "4g"
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_ARGS \
  -Xmx$JAVA_HEAP_SIZE -Xms$JAVA_HEAP_SIZE $JAVA_OPTS \
 -Dmy.docker.enabled=true \
 -Dmy.grafana.service=$MY_GRAFANA_SERVICE \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -jar application.jar
