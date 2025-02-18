#!/bin/bash

PROJECT=clickstream
MODULE=hazelcast
CLONE=2

BASEDIR=`dirname $0`
cd $BASEDIR/../../../$MODULE

HOST_IP=`ifconfig | grep -v 127.0.0.1 | grep -w inet -m 1 | cut -d" " -f2`
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

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

VOLUME_BASE=~/Downloads/volumes-${PROJECT}/${MODULE}${CLONE}
VOLUMES="-v ${VOLUME_BASE}:/basedir"

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}

PORT=$(($CLONE + 5701))

CMD="docker run \
 -e LOCAL_CLUSTER_NAME=blue \
 -e LOCAL_NODE_NAME=${MODULE}blue$CLONE \
 -e MY_CASSANDRA_CONTACT_POINTS=${HOST_IP}:9042 \
 -e MC_CLUSTER1_PORTLIST=5701 \
 -e MC_CLUSTER2_PORTLIST=6701 \
 -e MY_KUBERNETES_ENABLED=false \
 -e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:${PORT} \
 -p ${PORT}:${PORT} ${VOLUMES} --name=${MODULE}blue${CLONE} --rm --network=${PROJECT} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
