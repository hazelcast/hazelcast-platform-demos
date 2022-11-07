#!/bin/bash

PROJECT=transaction-monitor
MODULE=postgres

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# External port 8083
CMD="docker run -p 5432:5432 --rm --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

