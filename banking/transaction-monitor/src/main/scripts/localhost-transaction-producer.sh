#!/bin/bash

PROJECT=transaction-monitor
MODULE=transaction-producer

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

JAR_FILE=${PROJECT}-${MODULE}-5.3-jar-with-dependencies.jar

JAVA_ARGS="-Dmy.kubernetes.enabled=false"
JAVA_ARGS="${JAVA_ARGS} -Dhazelcast.local.publicAddress=${HOST_IP}"
JAVA_ARGS="${JAVA_ARGS} -Dmy.bootstrap.servers=${HOST_IP}:9092,${HOST_IP}:9093,${HOST_IP}:9094"
JAVA_ARGS="${JAVA_ARGS} -Dmy.pulsar.list=${HOST_IP}"

JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD="java $JAVA_ARGS $JAVA_OPTS -jar target/$JAR_FILE"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
