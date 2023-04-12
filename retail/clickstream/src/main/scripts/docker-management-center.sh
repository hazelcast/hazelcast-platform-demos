#!/bin/bash

PROJECT=clickstream
MODULE=management-center 

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

# Private network so can use container names
docker network create $PROJECT --driver bridge > /dev/null 2>&1
# For easier restarts
docker container prune --force > /dev/null 2>&1

DOCKER_IMAGE=hazelcast-platform-demos/${PROJECT}-${MODULE}
MC_CLUSTER1_ADDRESSLIST_OVERRIDE=${HOST_IP}:5701
MC_CLUSTER2_ADDRESSLIST_OVERRIDE=${HOST_IP}:6701

CMD="docker run \
 -e HOST_IP=$HOST_IP \
 -e MC_CLUSTER1_NAME=blue \
 -e MC_CLUSTER1_ADDRESSLIST=$HOST_IP \
 -e MC_CLUSTER1_PORTLIST=5701,5702 \
 -e MC_CLUSTER2_NAME=green \
 -e MC_CLUSTER2_ADDRESSLIST=$HOST_IP \
 -e MC_CLUSTER2_PORTLIST=6701,6702 \
 -p 8080:8080 --rm --network=${PROJECT} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
