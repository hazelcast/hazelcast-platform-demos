FROM amd64/ubuntu:22.04

# Add Java to Ubuntu image
RUN apt-get update && \
        apt-get install -y wget
RUN cd /tmp ; \
    wget -q https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz ; \
    tar xf openjdk-21_linux-x64_bin.tar.gz ;
RUN update-alternatives --install /usr/bin/java java /tmp/jdk-21/bin/java 1

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values, provide from kubernetes-1.yaml
ENV JAVA_ARGS=""
ENV JAVA_HEAP_SIZE="20g"
ENV MY_COOPERATIVE_THREAD_COUNT=""
ENV MY_GC_LOGGING=""
ENV MY_GC_TUNING=""
ENV MY_PARTITIONS=""

ENV JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && export JAVA_ARGS=\"-Xmx${JAVA_HEAP_SIZE} -Xms${JAVA_HEAP_SIZE} ${JAVA_ARGS}\" \
      && echo MY_GC_LOGGING=${MY_GC_LOGGING} \
      && echo MY_GC_TUNING=${MY_GC_TUNING} \
      && echo JAVA_ARGS=${JAVA_ARGS} \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo java -Dmy.partitions=${MY_PARTITIONS} -Dmy.cooperative.thread.count=${MY_COOPERATIVE_THREAD_COUNT} \
       $JAVA_ARGS $JAVA_OPTS $MY_GC_LOGGING $MY_GC_TUNING -jar application.jar \
      && java -Dmy.partitions=${MY_PARTITIONS} -Dmy.cooperative.thread.count=${MY_COOPERATIVE_THREAD_COUNT} \
       $JAVA_ARGS $JAVA_OPTS $MY_GC_LOGGING $MY_GC_TUNING -jar application.jar \
     "]
