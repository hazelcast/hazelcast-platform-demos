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
ARG JAR_FILE

# Maven compiled target
COPY target/${JAR_FILE} application.jar

# Need existing file so Docker copy does not fail if no keystore/truststore found. Put in target by maven-antrun-plugin
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
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo java --version \
      && java --version \
      && echo @@@@@@@@@@ \
      && chmod 755 /custom-entrypoint.sh \
      && echo /custom-entrypoint.sh \
      && /custom-entrypoint.sh \
     "]
