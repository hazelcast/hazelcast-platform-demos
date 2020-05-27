#!/bin/bash

PROJECT=cva
MODULE=grafana

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

echo Module $MODULE is only available to be run in Docker, using ${DOCKER_IMAGE}
RC=1
echo RC=${RC}
exit ${RC}
