#!/bin/bash
# 
BASEDIR=`dirname $0`

GROUP=hazelcast-platform-demos
PROJECT=transaction-monitor
TARGET=`basename $0 | sed 's/^docker-//' | sed 's/\.sh$//'`
CONTAINER_NAME=$TARGET
FIRST=`echo $TARGET | cut -d- -f1`
SECOND=`echo $TARGET | cut -d- -f2`
MODULE=${FIRST}
if [ "$FIRST" != "$SECOND" ] && [ $(expr `echo $SECOND | egrep -vw '0|1|2' | wc -l`) -gt 0 ]
then
 MODULE=${FIRST}-${SECOND}
fi

cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh
if [ $? -ne 0 ]
then
 exit
fi

# May need host machine IP for clustering
OS=`uname -s`
if [ "$OS" = "Darwin" ]; then
    HOST_IP=`ifconfig | grep -v 127.0.0.1 | grep -w inet -m 1 | cut -d" " -f2`
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

# Module specific
if [ "$FIRST" == "cassandra" ]
then
 DOCKER_ARGS="-e CASSANDRA_BROADCAST_ADDRESS=${HOST_IP}"
fi
if [ "$FIRST" == "mongo" ] && [ "$SECOND" == "updater" ]
then
 DOCKER_ARGS="-e HOST_IP=${HOST_IP}"
fi

# Internal/external port mapping
if [ "$DOCKER_PORT_INTERNAL" == "" ]
then
 PORT_MAPPING=""
else
 PORT_MAPPING="-p ${DOCKER_PORT_EXTERNAL}:${DOCKER_PORT_INTERNAL}"
fi

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

DOCKER_IMAGE=${GROUP}/${PROJECT}-${FLAVOR}-${MODULE}
CMD="docker run \
 ${DOCKER_ARGS} \
 ${PORT_MAPPING} --rm --name=${CONTAINER_NAME} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
