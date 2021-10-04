#!/bin/bash
# 
cd `dirname $0`
GROUP=hazelcast-platform-demos
PROJECT=booking
TARGET=`basename $0 | sed 's/^turbine-//' | sed 's/\.sh$//'`
CONTAINER_NAME=$TARGET
FIRST=`echo $TARGET | cut -d- -f1`
SECOND=`echo $TARGET | cut -d- -f2`
MODULE=${FIRST}
if [ "$FIRST" != "$SECOND" ]
then
 MODULE=${FIRST}-${SECOND}
fi

LOGFILE=/tmp/`basename $0`.$$
turbine setup --no-mc --no-trace > $LOGFILE 2>&1
TURBINE_SETUP_RC=$?
ALREADY=`grep -c "turbine is already running" $LOGFILE`
if [ $TURBINE_SETUP_RC -gt 0 ] && [ $ALREADY -ne 1 ]
then
 echo Problem starting Turbine
 echo ========================
 cat $LOGFILE
 exit
fi
TURBINE_MC=`docker ps | grep -c turbine-management-center`
if [ $TURBINE_MC -gt 0 ]
then
 echo Turbine Management Center found, use \""--no-mc\""
 exit
fi
TURBINE_ZK=`docker ps | grep -c turbine-zipkin`
if [ $TURBINE_ZK -gt 0 ]
then
 echo Turbine Zipkin found, use \""--no-trace\""
 exit
fi
echo Turbine confirmed as running
/bin/rm $LOGFILE > /dev/null 2>&1

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

# Services
if [ `echo $FIRST` == "hotel" ] 
then
 ENV="-e HOST_IP=${HOST_IP}"
 # For CURL debugging
 #ENV="-e HOST_IP=${HOST_IP} -l $DOCKER_PORT"
fi

# Internal/external port mapping
if [ "$DOCKER_PORT" == "" ]
then
 PORT_MAPPING=""
else
 PORT_MAPPING="-p ${DOCKER_PORT}"
fi

DOCKER_IMAGE=${GROUP}/${PROJECT}-${MODULE}
CMD="turbine run --no-pull ${DOCKER_IMAGE} ${PORT_MAPPING} ${ENV} --timeout=60000"
echo $CMD

$CMD
RC=$?
echo RC=${RC}
exit ${RC}
