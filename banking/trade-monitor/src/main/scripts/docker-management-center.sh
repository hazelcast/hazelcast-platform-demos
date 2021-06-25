#!/bin/bash

PROJECT=trade-monitor
MODULE=management-center 

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

echo '#################################################################################'
echo '#' Modify cluster config to use ${HOST_IP} and give it a few seconds to refresh
echo '#################################################################################'

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

CMD="docker run -e JAVA_ARGS=-Dhazelcast.mc.healthCheck.enable=true -p 8080:8080 --rm ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
