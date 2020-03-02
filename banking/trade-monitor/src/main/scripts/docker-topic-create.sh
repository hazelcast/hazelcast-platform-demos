#!/bin/bash

PROJECT=trade-monitor
MODULE=topic-create

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

MY_BOOTSTRAP_SERVERS=kafka-broker0:9092,kafka-broker1:9093,kafka-broker2:9094

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

# External port 8083
CMD="docker run -e MY_BOOTSTRAP_SERVERS=$MY_BOOTSTRAP_SERVERS --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

