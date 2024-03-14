#!/bin/bash

START=`date +"%y-%m-%d-%H-%M-%S"`

DIRNAME=`dirname $0`
cd $DIRNAME
TOP_LEVEL_DIR=`cd ../../../../.. ; pwd`
TOP_LEVEL_POM=$TOP_LEVEL_DIR/pom.xml
POM_FLAVOR=`grep '<my.transaction-monitor.flavor>' ../../../../../pom.xml | tail -1 | cut -d'>' -f2 | cut -d'<' -f1`
POM_USE_VIRIDIAN=`grep '<use.viridian>' $TOP_LEVEL_POM | tail -1 | cut -d'>' -f2 | cut -d'<' -f1 | tr '[:upper:]' '[:lower:]'`

# For Kubernetes cluster creation
K_MACHINE_TYPE="c2-standard-4"
K_NETWORK="neil-europe-west1"
K_NETWORK_SUBNET="neil-europe-west1-subnet"
K_NUM_NODES=15
K_PROJECT=hazelcast-33
K_ZONE=europe-west1-d

ARG1=`echo $1 | awk '{print tolower($0)}'`
ARG2=`echo $2 | awk '{print tolower($0)}'`
ARG3=`echo $3 | awk '{print tolower($0)}'`

# Use top level pom.xml values if no args
if [ "${ARG1}" == "" ]
then
 FLAVOR=$POM_FLAVOR
 USE_VIRIDIAN=$POM_USE_VIRIDIAN
 CREATE_KUBERNETES_CLUSTER=true
else
 if [ "${ARG1}" == "ecommerce" ]
 then
  FLAVOR=ecommerce
 fi
 if [ "${ARG1}" == "payments" ]
 then
  FLAVOR=payments
 fi
 if [ "${ARG1}" == "trade" ]
 then
  FLAVOR=trade
 fi

 # False if absent
 if [ "${ARG2}" == "true" ]
 then
  USE_VIRIDIAN=true
 else
  USE_VIRIDIAN=false
 fi

 # True if absent
 if [ "${ARG3}" == "false" ]
 then
  CREATE_KUBERNETES_CLUSTER=false
 else
  CREATE_KUBERNETES_CLUSTER=true
 fi
fi

if [ "${FLAVOR}" == "" ]
then
 echo $0: usage: `basename $0`
 echo $0: usage: `basename $0` '<flavor>' '<viridian>' '<create-cluster>'
 echo $0: eg: `basename $0`
 echo $0: ' ' to use top-level pom.xml values
 echo $0: eg: `basename $0` ecommerce true false
 echo $0: ' ' to use specific values
 exit 1
fi

echo ============================================================
echo Attempts to do all steps for Google Cloud
echo ============================================================
echo Flavor: $FLAVOR
echo Use-Viridian: $USE_VIRIDIAN
echo Create-Kubernetes-Cluster: $CREATE_KUBERNETES_CLUSTER

if [ "${FLAVOR}" != "${POM_FLAVOR}" ] || [ "${USE_VIRIDIAN}" != "${POM_USE_VIRIDIAN}" ]
then
 echo '************************************************************'
 echo '************************************************************'
 echo $TOP_LEVEL_POM is configured with FLAVOR=$POM_FLAVOR and USE_VIRIDIAN=$POM_USE_VIRIDIAN
 echo '************************************************************'
 echo '************************************************************'
 echo -n Proceeding in 10 seconds
 COUNTDOWN=10
 while [ $COUNTDOWN -gt 0 ]
 do
  echo -n .
  sleep 1
  COUNTDOWN=$(($COUNTDOWN - 1))
 done
 echo ""
fi

echo ----
echo $START | cut -d- -f4- | tr '-' ':'
echo ----

PROJECT=transaction-monitor
# Wait at most MAX_COUNT lots of SLEEPTIME for pod to start
MAX_COUNT=20
SLEEPTIME=30
TMPFILE=/tmp/`basename $0`.$$

# Waits until POD is in the required state
wait_for_pod() {
 SCRIPT=`echo $1 | cut -d- -f2`
 case $SCRIPT in
  1)
   WAIT_ON=zookeeper
   REQUIRED_STATE=Running
   ;;
  3)
   WAIT_ON=kafka-broker-2 
   REQUIRED_STATE=Running
   ;;
  4)
   WAIT_ON=job-topic-create
   REQUIRED_STATE=Completed
   ;;
  5)
   WAIT_ON=live-hazelcast-2
   REQUIRED_STATE=Running
   ;;
  6)
   WAIT_ON=webapp
   REQUIRED_STATE=Running
   ;;
  7)
   WAIT_ON=job-trans-producer
   REQUIRED_STATE=Running
   ;;
  8)
   WAIT_ON=client-python
   REQUIRED_STATE=Running
   ;;
 esac
 echo Waiting for \'$WAIT_ON\' to reach \"$REQUIRED_STATE\" state.
 COUNT=0
 READY="false"
 POD=${PROJECT}-${FLAVOR}-$WAIT_ON
 while [ $COUNT -lt $MAX_COUNT ] && [ "$READY" == "false" ]
 do
  COUNT=$(($COUNT + 1))
  STATUS=`kubectl get pods 2>&1  | grep ^$POD`
  if [ "$STATUS" != "" ]
  then
   ACTUAL_STATE=`echo $STATUS | awk '{print $3}'`
   POD=`echo $STATUS | awk '{print $1}'`
   if [ "$ACTUAL_STATE" == "$REQUIRED_STATE" ]
   then
    echo `date +"%H:%M:%S"`: Pod \'$POD\' in expected state
    READY=true
   else
    echo `date +"%H:%M:%S"`: Pod \'$POD\' in state \'$ACTUAL_STATE\', waiting
    sleep $SLEEPTIME
   fi
  else
   echo `date +"%H:%M:%S"`: Pod \'$POD\' not yet started
   sleep $SLEEPTIME
  fi
 done
 if [ "$READY" == "false" ]
 then
  echo `date +"%H:%M:%S"`: Did not reach required state in $COUNT loops of $SLEEPTIME seconds
  exit 0
 fi
}

# Applies a script that builds a "kubectl" input
do_cmd() {
 CMD="$1"
 ARG="$2"
 COUNT=0
 READY="false"
 POD=${PROJECT}-$WAIT_ON
 while [ $COUNT -lt $MAX_COUNT ] && [ "$READY" == "false" ]
 do
  COUNT=$(($COUNT + 1))
  echo $CMD $ARG
  echo ----
  ./$CMD $ARG > $TMPFILE.$CMD 2>&1
  KUBECTL_FILE=`grep ^"kubectl delete" $TMPFILE.$CMD | awk '{print $4}'`
  /bin/rm $TMPFILE.$CMD > /dev/null 2>&1
  if [ "$KUBECTL_FILE" == "" ]
  then
    echo `date +"%H:%M:%S"`: \"kubectl svc\" not yet ready, waiting
    sleep $SLEEPTIME
  else
    kubectl delete -f $KUBECTL_FILE > /dev/null 2>&1
    echo kubectl create -f $KUBECTL_FILE
    echo ----
    kubectl create -f $KUBECTL_FILE
    RC=$?
    echo ----
    if [ $RC -eq 0 ]
    then
     READY=true
    else
     Command failed with RC=$RC
     exit 0
    fi
    /bin/rm $KUBECTL_FILE > /dev/null 2>&1
  fi
 done
 if [ "$READY" == "false" ]
 then
  echo `date +"%H:%M:%S"`: Did not reach required state in $COUNT loops of $SLEEPTIME seconds
  exit 0
 fi
}

# Waits for Kubernetes cluster to be running
wait_for_cluster() {
 CLUSTER=$1
 REQUIRED_STATE=RUNNING
 echo Waiting for \'$CLUSTER\' to reach \"$REQUIRED_STATE\" state.
 COUNT=0
 READY="false"
 while [ $COUNT -lt $MAX_COUNT ] && [ "$READY" == "false" ]
 do
  COUNT=$(($COUNT + 1))
  STATUS=`gcloud container clusters list | grep $CLUSTER | awk '{print $8}'`
  if [ "$STATUS" != "" ]
  then
   if [ "$STATUS" == "$REQUIRED_STATE" ]
   then
    echo `date +"%H:%M:%S"`: Cluster \'$CLUSTER\' in expected state
    READY=true
   else
    echo `date +"%H:%M:%S"`: Cluster \'$CLUSTERD\' in state \'$STATUS\', waiting
    sleep $SLEEPTIME
   fi
  else
   echo `date +"%H:%M:%S"`: Cluster \'$CLUSTER\' not yet created
   sleep $SLEEPTIME
  fi
 done
 if [ "$READY" == "false" ]
 then
  echo `date +"%H:%M:%S"`: Did not reach required state in $COUNT loops of $SLEEPTIME seconds
  exit 0
 fi
}

# Check connected to Gcloud
CHECK=`gcloud container clusters list 2>&1`
if [ $? -ne 0 ]
then
 echo Problem connecting to GCloud: $CHECK
 exit 0
fi

# Create Kubernetes cluster
K_CLUSTER_NAME=${USER}-${FLAVOR}-${START}
if [ "$CREATE_KUBERNETES_CLUSTER" == true ]
then
 echo ================================================================================
 echo Attempting to create Kubernetes cluster : "$K_CLUSTER_NAME" 
 echo ================================================================================

 CREATE_CMD="gcloud container clusters create $K_CLUSTER_NAME \
  --enable-ip-alias \
  --machine-type $K_MACHINE_TYPE \
  --network projects/$K_PROJECT/global/networks/$K_NETWORK \
  --subnetwork projects/$K_PROJECT/regions/europe-west1/subnetworks/$K_NETWORK_SUBNET \
  --num-nodes $K_NUM_NODES \
  --project $K_PROJECT \
  --zone $K_ZONE"
 echo `date +"%H:%M:%S"` - Creating, may take a while, usually about 6-7 minutes.
 echo $CREATE_CMD
 CREATE=`$CREATE_CMD 2>&1`
 RC=$?
 echo `date +"%H:%M:%S"` - RC=$RC
 if [ $RC -ne 0 ]
 then
  echo Problem create Kubernetes cluster in GCloud: $CREATE
  exit 0
 fi

 wait_for_cluster $K_CLUSTER_NAME
 RC=$?
 echo RC=$RC
 if [ $RC -ne 0 ]
 then 
  echo RC=$RC, exiting
  exit 0
 fi
else
 RC=0
fi

# Apply the files in order
if [ "$USE_VIRIDIAN" == "true" ]
then
 FILES=`ls kubernetes* | grep -v kubernetes-5`
else
 FILES=`ls kubernetes* | grep -v kubernetes-5-optional-hazelcast.yaml`
fi

if [ $RC == 0 ]
then
 for INPUT_FILE in $FILES
 do
  echo START: $INPUT_FILE
  echo ====
  OUTPUT_FILE=$TMPFILE.$INPUT_FILE
  IS_YAML=`echo $INPUT_FILE | grep -c yaml`
  if [ $IS_YAML -eq 1 ]
  then
   # Assumed naming standard to find image
   sed "s#image: \"hazelcast-platform-demos#image: \"europe-west1-docker.pkg.dev/hazelcast-33/${USER}#" < $INPUT_FILE | \
   sed "s#FLAVOR#${FLAVOR}#g" | \
   sed 's#imagePullPolicy: Never#imagePullPolicy: Always#' > ${OUTPUT_FILE}
   CMD="kubectl create -f $OUTPUT_FILE"
   echo $CMD
   echo ----
   $CMD
   echo ----
   wait_for_pod $INPUT_FILE
  else
   do_cmd $INPUT_FILE $FLAVOR
  fi
  rm $OUTPUT_FILE > /dev/null 2>&1
  echo ====
  echo END: $INPUT_FILE
 done
fi

echo `date +"%H:%M:%S"` - Done

/bin/rm $TMPFILE.* > /dev/null 2>&1
RC=0
echo RC=$RC
exit $RC

