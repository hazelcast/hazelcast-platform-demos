#!/bin/bash

PROJECT=cva
MODULE=grafana

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

echo '#################################################################################'
echo '# Start from http://localhost:80            HTTP not HTTPS until logged in'
echo '#################################################################################'

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

CMD="docker run -p 80:80 -p 2004:2004 --rm --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
