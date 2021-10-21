FROM library/openjdk:17-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# From pom.xml
ARG HZ_CLOUD_CLUSTER1_DISCOVERY_TOKEN
ARG HZ_CLOUD_CLUSTER1_NAME

# Preconfigure
ENV HZ_CLOUD_CLUSTER1_DISCOVERY_TOKEN=$HZ_CLOUD_CLUSTER1_DISCOVERY_TOKEN
ENV HZ_CLOUD_CLUSTER1_NAME=$HZ_CLOUD_CLUSTER1_NAME
ENV JAVA_ARGS ""
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java \
   -DCLUSTER_DISCOVERY_TOKEN=$HZ_CLOUD_CLUSTER1_DISCOVERY_TOKEN \
   -DCLUSTER_NAME=$HZ_CLOUD_CLUSTER1_NAME \
   $JAVA_ARGS $JAVA_OPTS \
   -jar application.jar
