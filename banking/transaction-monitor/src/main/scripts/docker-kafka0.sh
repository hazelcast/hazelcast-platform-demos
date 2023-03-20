#!/bin/bash

PROJECT=transaction-monitor
MODULE=kafka-broker
CLONE=0

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

PORT=$(($CLONE + 9092))

CMD="docker run -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -p ${PORT}:9092 --name=${MODULE}${CLONE} --rm --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

