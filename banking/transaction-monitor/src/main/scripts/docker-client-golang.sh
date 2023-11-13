#!/bin/bash

PROJECT=transaction-monitor
MODULE=client-golang

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

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

CMD="docker run \
 -e HOST_IP=${HOST_IP} \
 -e MY_KUBERNETES_ENABLED=false \
 --rm ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

