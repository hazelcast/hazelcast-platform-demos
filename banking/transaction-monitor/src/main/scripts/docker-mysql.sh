#!/bin/bash

PROJECT=transaction-monitor
MODULE=mysql

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE
. ../src/main/scripts/check-flavor.sh

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${FLAVOR}-${MODULE}

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

# External port 3306
CMD="docker run -p 3306:3306 --rm --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

