FROM --platform=linux/amd64 library/openjdk:21-slim

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_MONGO_ADDRESS ""

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
 -Dmy.mongo.address=$MY_MONGO_ADDRESS \
 -jar application.jar \
      && java $JAVA_ARGS $JAVA_OPTS \
 -DHOST_IP=$HOST_IP \
 -Dmy.mongo.address=$MY_MONGO_ADDRESS \
 -jar application.jar \
     "]

