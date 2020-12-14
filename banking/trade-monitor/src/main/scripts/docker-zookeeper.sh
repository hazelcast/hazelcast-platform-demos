#!/bin/bash

PROJECT=trade-monitor
MODULE=zookeeper

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# Private network so can use container names
docker network rm $PROJECT > /dev/null 2>&1
docker network create $PROJECT --driver bridge

CMD="docker run -e ALLOW_ANONYMOUS_LOGIN=yes --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

