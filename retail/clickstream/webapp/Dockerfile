FROM amd64/ubuntu:22.04

# Add and confirm Java installed
RUN apt upgrade
RUN apt update
RUN apt-get install -y wget
# Add Java
RUN apt update
RUN cd /tmp ; \
    wget -q https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_linux-x64_bin.tar.gz ; \
    tar xf openjdk-17_linux-x64_bin.tar.gz ;
RUN update-alternatives --install /usr/bin/java java /tmp/jdk-17/bin/java 1

# From pom.xml
ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST
ARG MC_CLUSTER1_PORTLIST
ARG MC_CLUSTER2_NAME
ARG MC_CLUSTER2_ADDRESSLIST
ARG MC_CLUSTER2_PORTLIST

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV HOST_IP ""
ENV MY_KUBERNETES_ENABLED "true"

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MC_CLUSTER1_PORTLIST=$MC_CLUSTER1_PORTLIST
ENV MC_CLUSTER2_NAME=$MC_CLUSTER2_NAME
ENV MC_CLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST
ENV MC_CLUSTER2_PORTLIST=$MC_CLUSTER2_PORTLIST

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
      && echo exec java \
   $JAVA_ARGS $JAVA_OPTS \
   -DHOST_IP=${HOST_IP} \
   -DCLUSTER1_NAME=$MC_CLUSTER1_NAME \
   -DCLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST \
   -DCLUSTER1_PORTLIST=$MC_CLUSTER1_PORTLIST \
   -DCLUSTER2_NAME=$MC_CLUSTER2_NAME \
   -DCLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST \
   -DCLUSTER2_PORTLIST=$MC_CLUSTER2_PORTLIST \
   -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
   -jar application.jar \
      && exec java \
   $JAVA_ARGS $JAVA_OPTS \
   -DHOST_IP=${HOST_IP} \
   -DCLUSTER1_NAME=$MC_CLUSTER1_NAME \
   -DCLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST \
   -DCLUSTER1_PORTLIST=$MC_CLUSTER1_PORTLIST \
   -DCLUSTER2_NAME=$MC_CLUSTER2_NAME \
   -DCLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST \
   -DCLUSTER2_PORTLIST=$MC_CLUSTER2_PORTLIST \
   -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
   -jar application.jar \
     "]
