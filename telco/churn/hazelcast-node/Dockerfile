FROM --platform=linux/amd64 library/python:3.8-buster

# Add Java
RUN apt update
RUN cd /tmp ; \
    wget -q https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz ; \
    tar xf openjdk-21_linux-x64_bin.tar.gz ;
RUN update-alternatives --install /usr/bin/java java /tmp/jdk-21/bin/java 1

# Download requirements into image makes start up faster when growing cluster
COPY target/classes/requirements.txt /
RUN pip install --upgrade pip
RUN pip install -r ./requirements.txt

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default values provided
ENV MY_BOOTSTRAP_SERVERS="0.0.0.0:9092,0.0.0.0:9093,0.0.0.0:9094"
ENV MY_ENVIRONMENT="preprod"
ENV MY_INITSIZE=""
ENV MY_KUBERNETES_ENABLED="true"

ENV JAVA_ARGS=""
ENV JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_ARGS $JAVA_OPTS -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS -Dmy.environment=$MY_ENVIRONMENT -Dmy.initSize=$MY_INITSIZE -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED -jar application.jar
