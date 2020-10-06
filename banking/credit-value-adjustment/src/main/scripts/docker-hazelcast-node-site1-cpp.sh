#!/bin/bash

PROJECT=cva
MODULE=hazelcast-node-site1-cpp

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

# 3 CPUs, one each for Jet and 2 x C++ workers
PORT=50001
CMD="docker run --cpuset-cpus=0-2 -e MY_GRAFANA_SERVICE=grafana -e MY_INITSIZE=1 -e MY_KUBERNETES_ENABLED=false -e MY_PARTITIONS=271 -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP} -p 5701:5701 --network=${PROJECT} ${DOCKER_IMAGE} ${PORT}"
#echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
