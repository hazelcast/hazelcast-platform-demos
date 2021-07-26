#!/bin/bash
# 
cd `dirname $0`
GROUP=hazelcast-platform-demos
PROJECT=booking
TARGET=`basename $0 | sed 's/^docker-//' | sed 's/\.sh$//'`
CONTAINER_NAME=$TARGET
FIRST=`echo $TARGET | cut -d- -f1`
SECOND=`echo $TARGET | cut -d- -f2`
MODULE=${FIRST}
if [ "$FIRST" != "$SECOND" ] && [ $(expr `echo $SECOND | egrep -vw '0|1|2' | wc -l`) -gt 0 ]
then
 MODULE=${FIRST}-${SECOND}
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

# Server
if [ "$FIRST" == "hazelcast" ] && [ "$SECOND" == "node" ]
then
 JAVA_ARGS="-e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:${DOCKER_PORT_EXTERNAL}"
 JAVA_ARGS="${JAVA_ARGS} -e MY_KUBERNETES_ENABLED=false"
fi
# Client
if [ "$FIRST" == "hazelcast" ] && [ "$SECOND" != "node" ]
then
 JAVA_ARGS="-e HOST_IP=${HOST_IP}"
 JAVA_ARGS="${JAVA_ARGS} -e MY_KUBERNETES_ENABLED=false"
fi
# Management Center, convert member addresses
if [ "$FIRST" == "management" ] && [ "$SECOND" == "center" ]
then
 JAVA_ARGS="-e HOST_IP=${HOST_IP}"
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
CMD="docker run ${JAVA_ARGS} ${PORT_MAPPING} --rm --name=${CONTAINER_NAME} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
