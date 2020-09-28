#!/bin/bash

PROJECT=churn
MODULE=kafka-broker
CLONE=0

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

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

PORT=$(($CLONE + 9092))

CMD="docker run -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -p ${PORT}:9092 --name=${MODULE}${CLONE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

