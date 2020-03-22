#!/bin/bash

PROJECT=cva
MODULE=data-loader

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

HOST_IP=`ifconfig | grep -w inet | grep -v 127.0.0.1 | cut -d" " -f2`
if [ "$HOST_IP" == "" ]
then
 HOST_IP=127.0.0.1
fi
if [ `echo $HOST_IP | wc -w` -ne 1 ]
then
 echo \$HOST_IP unclear:
 ifconfig | grep -w inet | grep -v 127.0.0.1
 exit 1
fi

DOCKER_IMAGE=hazelcast-${PROJECT}/${MODULE}

if [ "${1}" == "CVA_SITE2" ]
then
 CMD="docker run -e MY_KUBERNETES_ENABLED=false -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP} ${DOCKER_IMAGE} CVA_SITE2"
else
 CMD="docker run -e MY_KUBERNETES_ENABLED=false -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP} ${DOCKER_IMAGE}"
fi
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
