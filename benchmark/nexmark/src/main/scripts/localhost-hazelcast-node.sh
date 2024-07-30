#!/bin/bash

PROJECT=nexmark
MODULE=hazelcast-node

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

HOST_IP=`ifconfig | grep -w inet | grep -v 127.0.0.1 | cut -d" " -f2`
if [ "$HOST_IP" == "" ]
then
 HOST_IP=127.0.0.1
fi
if [ `echo $HOST_IP | wc -w` -ne 1 ]
then
 echo \$HOST_IP unclear:
 ifconfig | grep -w inet | grep -v 127.0.0.1
 exit 1
fi

JAR_FILE=${PROJECT}-${MODULE}-5.5-jar-with-dependencies.jar

JAVA_ARGS="-Dhazelcast.config="`pwd`/../src/main/scripts/hazelcast.xml
JAVA_ARGS="${JAVA_ARGS} -Dmy.cooperative.thread.count=1"
JAVA_ARGS="${JAVA_ARGS} -Dhost.ip=$HOST_IP"
JAVA_ARGS="${JAVA_ARGS} -Dmy.partitions=271"
JAVA_ARGS="${JAVA_ARGS} -Xlog:safepoint,gc*,gc+ergo=trace,gc+age=trace,gc+phases=trace,gc+humongous=trace:file=/tmp/verbosegc.log:level,tags,time,uptime:filesize=1048576000,filecount=15"

JAVA_OPTS="\
 --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
 --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED \
 --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD="java $JAVA_ARGS $JAVA_OPTS -jar target/$JAR_FILE"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
