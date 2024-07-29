#!/bin/bash

PROJECT=nexmark
MODULE=webapp
HOST_IP=$1

JAR_FILE=${PROJECT}-${MODULE}-5.5.jar
JAVA_ARGS="-Dhazelcast.client.config="$HOME/hazelcast-client.xml
JAVA_ARGS="${JAVA_ARGS} -Dhost.ip=$HOST_IP"

JAVA_OPTS="\
 --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
 --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED \
 --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD="java $JAVA_ARGS $JAVA_OPTS -jar $JAR_FILE"
echo $CMD

nohup $CMD > log.$MODULE 2>&1 &
RC=$?
echo RC=${RC}
exit ${RC}
