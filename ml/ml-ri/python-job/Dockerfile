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

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default true
ENV MY_KUBERNETES_ENABLED "true"

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
      && echo java $JAVA_ARGS $JAVA_OPTS -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED -jar application.jar $0 \
      && java $JAVA_ARGS $JAVA_OPTS -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED -jar application.jar $0 \
     "]
