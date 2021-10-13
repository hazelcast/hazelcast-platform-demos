FROM library/openjdk:17-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV MY_CPP_SERVICE "cva-cpp"
ENV MY_KUBERNETES_ENABLED "true"
ENV MY_SITE ""

ENV JAVA_ARGS ""
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.cpp.service=$MY_CPP_SERVICE \
 -Dmy.docker.enabled=true \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -Dmy.site=$MY_SITE \
 -jar application.jar $0
