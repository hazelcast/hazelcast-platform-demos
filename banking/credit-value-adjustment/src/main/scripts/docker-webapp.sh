#!/bin/bash

PROJECT=cva
MODULE=webapp

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

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

# External port 8081
CMD="docker run -e MY_CPP_SERVICE=cva-cpp -e MY_KUBERNETES_ENABLED=false -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP} -p 8081:8080 ${DOCKER_IMAGE} $@"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}

