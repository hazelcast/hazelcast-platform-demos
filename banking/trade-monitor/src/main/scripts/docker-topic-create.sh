#!/bin/bash

PROJECT=trade-monitor
MODULE=topic-create

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

MY_ZOOKEEPER=zookeeper:2181

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# External port 8083
CMD="docker run -e MY_ZOOKEEPER=$MY_ZOOKEEPER --rm --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

