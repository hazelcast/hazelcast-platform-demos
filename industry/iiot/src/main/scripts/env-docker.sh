#!/bin/bash
# 
cd `dirname $0`
GROUP=hazelcast-platform-demos
PROJECT=iiot
TARGET=`basename $0 | sed 's/^docker-//' | sed 's/\.sh$//'`
FIRST=`echo $TARGET | cut -d- -f1`
SECOND=`echo $TARGET | cut -d- -f2`
THIRD=`echo $TARGET | cut -d- -f3`
FOURTH=`echo $TARGET | cut -d- -f4`
MODULE=${FIRST}-${SECOND}-${THIRD}
CONTAINER_NAME=${MODULE}
VOLUME=""
if [ "$FIRST" != "test" ]
then
 echo Usage: ${0}: Only for "test"
 exit 1
fi
if [ "$FOURTH" != "" ] && [ $(expr `echo "$FOURTH" | egrep -w '0|1|2' | wc -l`) -gt 0 ]
then
 CONTAINER_NAME=${CONTAINER_NAME}-${FOURTH}
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

# Test Cluster
if [ "$THIRD" == "hazelcast" ]
then
 JAVA_ARGS="-e JAVA_ARGS=-Dhazelcast.local.publicAddress=${HOST_IP}:${DOCKER_PORT_EXTERNAL}"
fi
# Test Client
if [ "$THIRD" == "client" ]
then
 JAVA_ARGS="-e HOST_IP=${HOST_IP}"
fi

# Grafana,etc - part of main demo, image name does not include "test"
if [ "$THIRD" == "grafana" ] || [ "$THIRD" == "mongo" ] || [ "$THIRD" == "prometheus" ]
then
 MODULE=${THIRD}
fi

# Internal/external port mapping
if [ "$DOCKER_PORT_INTERNAL" == "" ]
then
 PORT_MAPPING=""
else
 PORT_MAPPING="-p ${DOCKER_PORT_EXTERNAL}:${DOCKER_PORT_INTERNAL}"
fi
if [ "$DOCKER_PORT_INTERNAL2" != "" ]
then
 PORT_MAPPING="${PORT_MAPPING} -p ${DOCKER_PORT_EXTERNAL2}:${DOCKER_PORT_INTERNAL2}"
fi

# So can rerun named container
docker container prune --force > /dev/null 2>&1

DOCKER_IMAGE=${GROUP}/${PROJECT}-${MODULE}
CMD="docker run ${JAVA_ARGS} ${VOLUME} ${PORT_MAPPING} --rm --name=${CONTAINER_NAME} ${DOCKER_IMAGE}"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
