#!/bin/bash

PROJECT=cva
MODULE=cva-cpp

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

# CPP Server on port 50001
PORT=50001
CMD="docker run -p ${PORT}:${PORT} --name=${MODULE} --network=${PROJECT} ${DOCKER_IMAGE} ${PORT}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

