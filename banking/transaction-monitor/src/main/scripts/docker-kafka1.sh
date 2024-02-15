#!/bin/bash

PROJECT=transaction-monitor
MODULE=kafka-broker
CLONE=1

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

PORT=$(($CLONE + 9092))

CMD="docker run -e ALLOW_PLAINTEXT_LISTENER=true \
-e KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL_PLAINTEXT \
-e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=EXTERNAL_PLAINTEXT:PLAINTEXT,INTERNAL_PLAINTEXT:PLAINTEXT,PLAINTEXT:PLAINTEXT \
-e KAFKA_CFG_LISTENERS=EXTERNAL_PLAINTEXT://:${PORT},INTERNAL_PLAINTEXT://:1${PORT} \
-e KAFKA_ADVERTISED_LISTENERS=EXTERNAL_PLAINTEXT://:${PORT},INTERNAL_PLAINTEXT://:1${PORT} \
-e KAFKA_ENABLE_KRAFT=no -e KAFKA_KRAFT_CLUSTER_ID=$FLAVOR \
-e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -p ${PORT}:${PORT} -p 1${PORT}:1${PORT} --name=${MODULE}${CLONE} --rm --network=${PROJECT} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

