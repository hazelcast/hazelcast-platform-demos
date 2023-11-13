#!/bin/bash

PROJECT=transaction-monitor
MODULE=hazelcast-node-enterprise-2
CLUSTER_NAME=dr
CLONE=0

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh

# Darwin vs Linux
OS=`uname -s`
if [ "$OS" = "Darwin" ]; then
    HOST_IP=`ifconfig | grep -w inet | grep 192.168.[012] | cut -d" " -f2`
fi

if [ "$OS" = "Linux" ]; then
    HOST_IP=`ifconfig | grep -w inet -m 1 | awk '{print $2}'`
fi

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

MY_BOOTSTRAP_SERVERS=kafka-broker0:9092,kafka-broker1:9093,kafka-broker2:9094
MY_CASSANDRA_ADDRESS=cassandra:9042
MY_MARIA_ADDRESS=maria:4306
MY_MONGO_ADDRESS=mongo:27017
MY_MYSQL_ADDRESS=mysql:3306
MY_POSTGRES_ADDRESS=postgres:5432
# See also $MEMORY. Need to allow at least 500MB for heap
MY_NATIVE_MEGABYTES=4400
MY_PULSAR_ADDRESS=pulsar:6650

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# Tiered Store
MEMORY="--memory=5g"
VOLUME_BASE=~/Downloads/volumes-${PROJECT}/${MODULE}
VOLUME_TIERED_STORAGE=${VOLUME_BASE}/${CLUSTER_NAME}
mkdir -p $VOLUME_TIERED_STORAGE
VOLUMES="-v ${VOLUME_BASE}:/data/${PROJECT}"

PORT=$(($CLONE + 5701))

CMD="docker run -e MY_BOOTSTRAP_SERVERS=$MY_BOOTSTRAP_SERVERS \
 -e MY_KUBERNETES_ENABLED=false \
 -e MY_CASSANDRA_ADDRESS=$MY_CASSANDRA_ADDRESS \
 -e MY_MARIA_ADDRESS=$MY_MARIA_ADDRESS \
 -e MY_MONGO_ADDRESS=$MY_MONGO_ADDRESS \
 -e MY_MYSQL_ADDRESS=$MY_MYSQL_ADDRESS \
 -e MY_NATIVE_MEGABYTES=$MY_NATIVE_MEGABYTES \
 -e MY_POSTGRES_ADDRESS=$MY_POSTGRES_ADDRESS \
 -e MY_PULSAR_ADDRESS=$MY_PULSAR_ADDRESS \
 -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:${PORT} \
 -p ${PORT}:${PORT} ${MEMORY} ${VOLUMES} --name=${MODULE}${CLONE} --rm --network=${PROJECT} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

