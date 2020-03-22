#!/bin/bash

PROJECT=cva
MODULE=grafana

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

# Private network so can use container names
docker network rm $PROJECT > /dev/null 2>&1
docker network create $PROJECT --driver bridge

CMD="docker run -p 80:80 -p 2004:2004 --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
