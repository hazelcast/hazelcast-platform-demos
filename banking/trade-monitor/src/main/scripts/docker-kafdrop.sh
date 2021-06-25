#!/bin/bash

PROJECT=trade-monitor
MODULE=kafdrop

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

MY_BOOTSTRAP_SERVERS=kafka-broker0:9092,kafka-broker1:9093,kafka-broker2:9094

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# External port 8083
CMD="docker run -e KAFKA_BROKERCONNECT=$MY_BOOTSTRAP_SERVERS -p 8083:9000 --rm --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

