FROM amd64/ubuntu:20.04

# Add Java 17 to Ubuntu image
RUN apt-get update && \
        apt-get install -y wget
RUN cd /tmp ; \
    wget -q https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_linux-x64_bin.tar.gz ; \
    tar xf openjdk-17_linux-x64_bin.tar.gz ; 
RUN update-alternatives --install /usr/bin/java java /tmp/jdk-17/bin/java 1
RUN java --version

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values, provide from kubernetes-2.yaml
ENV MY_PARTITIONS "1"

ENV JAVA_HEAP_SIZE "3g"
ENV JAVA_ARGS "-Xms$JAVA_HEAP_SIZE -Xmx$JAVA_HEAP_SIZE"
ENV JAVA_OPTS "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java \
   -Dmy.partitions=$MY_PARTITIONS \
   $JAVA_ARGS $JAVA_OPTS \
   -jar application.jar
