#!/bin/bash

PROJECT=churn
MODULE=java-client

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

HOST_IP=`ifconfig | grep -v 127.0.0.1 | grep -w inet -m 1 | cut -d" " -f2`
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

# External port 8081
CMD="docker run -e MY_KUBERNETES_ENABLED=false -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:5701 -p 8081:8080 --name=${MODULE} --rm --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
