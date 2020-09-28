#!/bin/bash

PROJECT=churn
MODULE=kafdrop

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

MY_BOOTSTRAP_SERVERS=kafka-broker0:9092,kafka-broker1:9093,kafka-broker2:9094

# External port 8083
CMD="docker run -e KAFKA_BROKERCONNECT=$MY_BOOTSTRAP_SERVERS -p 8083:9000 --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

