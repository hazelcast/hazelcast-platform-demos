#!/bin/bash

PROJECT=churn
MODULE=grafana

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

echo '#################################################################################'
echo '# Start from http://localhost:8081          HTTP not HTTPS until logged in'
echo '#################################################################################'

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

CMD="docker run -p 8081:3000 --name=${MODULE} ${DOCKER_IMAGE}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
