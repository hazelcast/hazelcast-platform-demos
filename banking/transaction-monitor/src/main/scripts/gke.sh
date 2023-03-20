#!/bin/bash

ARG1=`echo $1 | awk '{print tolower($0)}'`
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

if [ "${FLAVOR}" == "" ]
then
 echo $0: usage: `basename $0` '<flavor>'
 exit 1
fi

echo ============================================================
echo Attempts to do all steps for Google Cloud
echo ============================================================
echo `date +"%H:%M:%S"`
echo Flavor: $FLAVOR
echo ----

PROJECT=transaction-monitor
# Wait at most MAX_COUNT lots of SLEEPTIME for pod to start
MAX_COUNT=20
SLEEPTIME=30
TMPFILE=/tmp/`basename $0`.$$
DIRNAME=`dirname $0`
cd $DIRNAME

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
   WAIT_ON=grid1-hazelcast-2
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

# Apply the files in order
ls kubernetes* | grep -v kubernetes-5-optional-hazelcast.yaml | while read -r INPUT_FILE
do
 echo START: $INPUT_FILE
 echo ====
 OUTPUT_FILE=$TMPFILE.$INPUT_FILE
 IS_YAML=`echo $INPUT_FILE | grep -c yaml`
 if [ $IS_YAML -eq 1 ]
 then
  # Assumed naming standard to find image
  sed "s#image: \"hazelcast-platform-demos#image: \"eu.gcr.io/hazelcast-33/${USER}#" < $INPUT_FILE | \
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

echo `date +"%H:%M:%S"` - Done

/bin/rm $TMPFILE.* > /dev/null 2>&1
