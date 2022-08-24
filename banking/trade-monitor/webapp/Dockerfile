FROM library/openjdk:17-slim

ARG JAR_FILE

# Maven compiled target
COPY target/${JAR_FILE} application.jar

# Need existing file so Docker copy does not fail if not keystore/truststore found. Put in target by maven-antrun-plugin
COPY target/${JAR_FILE} target/client.keystore* /
COPY target/${JAR_FILE} target/client.truststore* /
RUN rm /${JAR_FILE}

# Default values provided
ENV MY_BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"
ENV MY_DOCKER_ENABLED "true"
ENV MY_KUBERNETES_ENABLED "true"

ENV JAVA_ARGS ""
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
