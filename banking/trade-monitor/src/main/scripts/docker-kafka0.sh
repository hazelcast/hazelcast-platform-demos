#!/bin/bash

PROJECT=trade-monitor
MODULE=kafka-broker
CLONE=0

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

PORT=$(($CLONE + 9092))

CMD="docker run -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -p ${PORT}:9092 --name=${MODULE}${CLONE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

