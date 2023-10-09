#!/bin/bash

PROJECT=transaction-monitor
MODULE=webapp

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh

# Darwin vs Linux
OS=`uname -s`
if [ "$OS" = "Darwin" ]; then
    HOST_IP=`ifconfig | grep -w inet | grep -v 127.0.0.1 | cut -d" " -f2`
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
MY_PULSAR_ADDRESS=pulsar:6650

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# External port 8081
CMD="docker run -e MY_BOOTSTRAP_SERVERS=$MY_BOOTSTRAP_SERVERS \
 -e MY_KUBERNETES_ENABLED=false \
<<<<<<< HEAD
 -e MY_CASSANDRA_ADDRESS=$MY_CASSANDRA_ADDRESS \
 -e MY_MARIA_ADDRESS=$MY_MARIA_ADDRESS \
 -e MY_MONGO_ADDRESS=$MY_MONGO_ADDRESS \
=======
>>>>>>> master
 -e MY_MYSQL_ADDRESS=$MY_MYSQL_ADDRESS \
 -e MY_POSTGRES_ADDRESS=$MY_POSTGRES_ADDRESS \
 -e MY_PULSAR_ADDRESS=$MY_PULSAR_ADDRESS \
 -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:5701 \
 -p 8080:8080 --rm ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

