#!/bin/bash

PROJECT=trade-monitor
MODULE=kafdrop

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

MY_BOOTSTRAP_SERVERS=kafka-broker0:9092,kafka-broker1:9093,kafka-broker2:9094

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

# External port 8083
CMD="docker run -e KAFKA_BROKERCONNECT=$MY_BOOTSTRAP_SERVERS -p 8083:9000 --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

