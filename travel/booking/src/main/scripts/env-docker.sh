#!/bin/bash
# 
cd `dirname $0`
GROUP=hazelcast-platform-demos
PROJECT=booking
TARGET=`basename $0 | sed 's/^docker-//' | sed 's/\.sh$//'`
CONTAINER_NAME=$TARGET
FIRST=`echo $TARGET | cut -d- -f1`
SECOND=`echo $TARGET | cut -d- -f2`
THIRD=`echo $TARGET | cut -d- -f3`
if [ "${FIRST}" == "${SECOND}" ]
then
 MODULE=${FIRST}
else
 MODULE=${FIRST}-${SECOND}
fi

# Private network so can use container names
if [ `docker network list | awk '{print $2}' | grep -c $PROJECT` -ne 1 ]
then
 docker network create $PROJECT --driver bridge > /dev/null 2>&1
fi

# May need host machine IP for clustering
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
DOCKERIP="-e HOST_IP=${HOST_IP}"

# Server
if [ "$FIRST" == "hazelcast" ] && [ "$SECOND" == "node" ]
then
 JAVA_ARGS="-e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:${DOCKER_PORT_EXTERNAL}"
 JAVA_ARGS="${JAVA_ARGS} -e LOCAL_NODE_NAME=member${THIRD}"
 DOCKERIP="-e HOST_IP=${HOST_IP} -e MY_KUBERNETES_ENABLED=false"
fi
# Client
if [ "$FIRST" == "hazelcast" ] && [ "$SECOND" != "node" ]
then
 JAVA_ARGS="-e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}"
 DOCKERIP="-e HOST_IP=${HOST_IP} -e MY_KUBERNETES_ENABLED=false"
fi

# Internal/external port mapping
if [ "$DOCKER_PORT_INTERNAL" == "" ]
then
 PORT_MAPPING=""
else
 PORT_MAPPING="-p ${DOCKER_PORT_EXTERNAL}:${DOCKER_PORT_INTERNAL}"
fi

# So can rerun named container
docker container prune --force > /dev/null 2>&1

DOCKER_IMAGE=${GROUP}/${PROJECT}-${MODULE}
CMD="docker run $TTY ${JAVA_ARGS} ${VOLUMES} ${PORT_MAPPING} ${DOCKERIP} --cap-add=sys_nice --rm --name=${CONTAINER_NAME} --network=${PROJECT} ${DOCKER_IMAGE} $*"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
