#!/bin/bash

PROJECT=trade-monitor
MODULE=topic-create

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

MY_ZOOKEEPER=zookeeper:2181

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# External port 8083
CMD="docker run -e MY_ZOOKEEPER=$MY_ZOOKEEPER --network=${PROJECT} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

