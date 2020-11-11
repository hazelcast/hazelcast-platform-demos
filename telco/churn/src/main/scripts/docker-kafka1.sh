#!/bin/bash

PROJECT=churn
MODULE=kafka-broker
CLONE=1

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
PORT2=$(($PORT + 100))

SECURITY_MAP=NORMAL:PLAINTEXT,NORMAL2:PLAINTEXT
ADVERTISED=NORMAL://${MODULE}${CLONE}:${PORT},NORMAL2://${MODULE}${CLONE}:${PORT2}
LISTENERS=$ADVERTISED
INTERBROKER=NORMAL

CMD="docker run -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_INTER_BROKER_LISTENER_NAME=${INTERBROKER} -e KAFKA_LISTENERS=${LISTENERS} -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=${SECURITY_MAP} -e KAFKA_CFG_ADVERTISED_LISTENERS=${ADVERTISED} -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -p ${PORT}:${PORT} -p ${PORT2}:${PORT2} --name=${MODULE}${CLONE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

