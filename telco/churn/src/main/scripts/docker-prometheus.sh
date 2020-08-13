#!/bin/bash

PROJECT=churn
MODULE=prometheus

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

CMD="docker run -p 9090:9090 --name=${MODULE} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
