FROM hazelcast-platform-demos/cva-cpp:latest

RUN apt update
RUN apt install -y openjdk-11-jdk
RUN java --version

COPY --from=hazelcast-platform-demos/cva-hazelcast-node-site1  application.jar /application.jar

# Wrapper script starts Jet node and gRPC server(s)
COPY target/classes/cvaWrapper.sh /cvaWrapper.sh
RUN chmod u+x /cvaWrapper.sh

# Default values provided
ENV MY_GRAFANA_SERVICE "cva-grafana"
ENV MY_KUBERNETES_ENABLED "true"
ENV MY_INITSIZE ""
ENV MY_PARTITIONS ""

ENV JAVA_ARGS ""
ENV JAVA_HEAP_SIZE "4g"
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec /cvaWrapper.sh $0 $1 \
  $JAVA_ARGS \
  -XX:+UseG1GC -XX:ActiveProcessorCount=1 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -XX:+ShowMessageBoxOnError \
  -Xmx$JAVA_HEAP_SIZE -Xms$JAVA_HEAP_SIZE $JAVA_OPTS \
 -Dmy.docker.enabled=true \
 -Dmy.grafana.service=$MY_GRAFANA_SERVICE \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -Dmy.initSize=$MY_INITSIZE \
 -Dmy.partitions=$MY_PARTITIONS
