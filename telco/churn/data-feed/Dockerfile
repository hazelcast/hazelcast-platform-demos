FROM library/openjdk:17-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV MY_BOOTSTRAP_SERVERS "0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"

ENV JAVA_ARGS ""
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS -jar application.jar
