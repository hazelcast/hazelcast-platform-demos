#!/bin/bash

PROJECT=nexmark
MODULE=hazelcast-node
HOST_IP=$1
PUBLIC_IP=$2

JAR_FILE=${PROJECT}-${MODULE}-5.3-jar-with-dependencies.jar

JAVA_ARGS="-Dhazelcast.config="$HOME/hazelcast.xml
JAVA_ARGS="${JAVA_ARGS} -Dmy.cooperative.thread.count=14"
JAVA_ARGS="${JAVA_ARGS} -Dhazelcast.local.publicAddress=$PUBLIC_IP"
JAVA_ARGS="${JAVA_ARGS} -Dhost.ip=$HOST_IP"
JAVA_ARGS="${JAVA_ARGS} -Dmy.partitions=271"
JAVA_ARGS="${JAVA_ARGS} -Xlog:safepoint,gc*,gc+ergo=trace,gc+age=trace,gc+phases=trace,gc+humongous=trace:file=/home/ubuntu/verbosegc.log:level,tags,time,uptime:filesize=1048576000,filecount=15"

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
