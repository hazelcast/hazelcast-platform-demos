#!/bin/bash

BASEDIR=`dirname $0`
cd $BASEDIR

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

PREFIX=transaction-monitor-${FLAVOR}
STATEFULSET_NAME=${PREFIX}-kafka-broker
SERVICE_NAMES=cassandra,flow,maria,mongo,mysql,postgres,pulsar
SERVICE_NAMES_COUNT=`echo $SERVICE_NAMES | tr ',' '\n' | grep -v '^$' | wc -l`
SERVICE_IPS=""
IPLIST=""
TMPFILE=/tmp/`basename $0`.$$

# 
captureIP() {
 SVC_NAME=${PREFIX}-${1}
 SVC_INFO=`kubectl get svc $SVC_NAME 2>&1`
 SVC_IP=""
 PENDING=`echo $SVC_INFO | egrep -c '<pending>'\|'<none>'`
 NOT_RUNNING=`echo $SVC_INFO | grep -c '(NotFound)'`
 if [ $PENDING -gt 0 ] || [ $NOT_RUNNING -gt 0 ]
 then
  echo ""
  echo `basename $0`: ERROR: Service \"$SVC_NAME\" not ready for IP capture: $SVC_INFO
  echo ""
  if [ $NOT_RUNNING -gt 0 ]
  then
   echo Was kubernetes-1-zookeeper-kafka-firsthalf.yaml run\?
  else
   echo Try again in 30 seconds\? '<pending>'/'<none>' will clear.
  fi
  echo ""
 else
  local TMP_IP=`kubectl get svc $SVC_NAME -o jsonpath='{.status.loadBalancer.ingress[0].ip}'`
  if [ "$TMP_IP" != "" ]
  then
   SVC_IP="$TMP_IP"
  fi
 fi
}

# Single services, try all to find which are running
while read SVC
do
 captureIP $SVC
 COMMA=""
 if [ "$SERVICE_IPS" != "" ]
 then
  COMMA=","
 fi
 SERVICE_IPS=${SERVICE_IPS}${COMMA}${SVC_IP}
done < <( echo $SERVICE_NAMES | tr ',' '\n')

SERVICE_IPS_COUNT=`echo $SERVICE_IPS | tr ',' '\n' | grep -v '^$' | wc -l`
if [ $SERVICE_NAMES_COUNT -ne $SERVICE_IPS_COUNT ]
then
 echo `basename $0`: ERROR: In services \"$SERVICE_NAMES\" missing some IPs
 echo `basename $0`: ERROR: Got \"$SERVICE_IPS\"
 exit 1
fi

# 3 Kafkas, each with their own LB
for REPLICA in 0 1 2
do
 SVC=${STATEFULSET_NAME}-${REPLICA}
 SVC_INFO=`kubectl get svc $SVC 2>&1`
 PENDING=`echo $SVC_INFO | egrep -c '<pending>'\|'<none>'`
 NOT_RUNNING=`echo $SVC_INFO | grep -c '(NotFound)'`
 if [ $PENDING -gt 0 ] || [ $NOT_RUNNING -gt 0 ]
 then
  echo ""
  echo `basename $0`: ERROR: Service \"$SVC\" not ready for IP capture: $SVC_INFO
  echo ""
  if [ $NOT_RUNNING -gt 0 ]
  then
   echo Was kubernetes-1-zookeeper-kafka-firsthalf.yaml run\?
  else
   echo Try again in 30 seconds\? '<pending>'/'<none>' will clear.
  fi
  echo ""
 else
  IP=`kubectl get svc $SVC -o jsonpath='{.status.loadBalancer.ingress[0].ip}'`
  if [ "$IP" != "" ]
  then
   if [ "$IPLIST" == "" ]
   then
    IPLIST="$IP"
   else
    IPLIST="$IPLIST,$IP"
   fi
  fi
 fi
done

IPCOUNT=`echo $IPLIST | sed 's/,/ /g' | wc -w`
if [ $IPCOUNT -ne 3 ]
then
 echo `basename $0`: ERROR: Need 3 IPs in Kafka IP list: \"$IPLIST\"
 exit 1
fi

echo "---" >> $TMPFILE
echo "# ConfigMap for Kafka broker pods, each uses broker id to find IP in external IP list" >> $TMPFILE
echo "---" >> $TMPFILE
echo "apiVersion: v1" >> $TMPFILE
echo "kind: ConfigMap" >> $TMPFILE
echo "metadata:" >> $TMPFILE
echo "  name: transaction-monitor-${FLAVOR}-configmap" >> $TMPFILE
echo "data:" >> $TMPFILE
echo "  # Creates 'my-env.sh' mounted in /customize in pod" >> $TMPFILE
echo "  my-env.sh: |-" >> $TMPFILE
echo "    #!/bin/bash" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
echo "    echo Running setup.sh" >> $TMPFILE
echo "    echo @--------------@" >> $TMPFILE
echo "    echo MY_POD_IP \"\$MY_POD_IP\"" >> $TMPFILE
echo "    echo MY_POD_NAME \"\$MY_POD_NAME\"" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
# $ID should be numeric - 0, 1 or 2
echo "    ID=\`echo \$MY_POD_NAME | sed s/transaction-monitor-${FLAVOR}-kafka-broker-//\`" >> $TMPFILE
echo "    EXTERNAL_PORT=9092" >> $TMPFILE
echo "    INTERNAL_PORT=19092" >> $TMPFILE
echo "    echo ID \"\$ID\"" >> $TMPFILE
echo "    IPLIST=$IPLIST" >> $TMPFILE
echo "    echo IPLIST \"\$IPLIST\"" >> $TMPFILE
ITEM=1
while [ $ITEM -le $SERVICE_NAMES_COUNT ]
do
 SERVICE_NAME_UC=`echo $SERVICE_NAMES | cut -d, -f$ITEM | tr '[a-z]' '[A-Z'`
 SERVICE_IP=`echo $SERVICE_IPS | cut -d, -f$ITEM | tr '[a-z]' '[A-Z'`
 echo "    ${SERVICE_NAME_UC}ADDRESS=${SERVICE_IP}" >> $TMPFILE
 echo "    echo ${SERVICE_NAME_UC}ADDRESS \"\$${SERVICE_NAME_UC}ADDRESS\"" >> $TMPFILE
 echo "    export MY_${SERVICE_NAME_UC}_ADDRESS=\${${SERVICE_NAME_UC}ADDRESS}" >> $TMPFILE
 echo "    echo Set: MY_${SERVICE_NAME_UC}_ADDRESS \"\$MY_${SERVICE_NAME_UC}_ADDRESS\"" >> $TMPFILE
 ITEM=$(($ITEM + 1))
done
echo "    IP0=\`echo \$IPLIST | cut -d, -f1\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    IP1=\`echo \$IPLIST | cut -d, -f2\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    IP2=\`echo \$IPLIST | cut -d, -f3\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    export KAFKA_CFG_BROKER_ID=\${ID}" >> $TMPFILE
echo "    INTERNAL=\${MY_POD_IP}:\${INTERNAL_PORT}" >> $TMPFILE
#echo "    INTERNAL=\${MY_POD_NAME}.transaction-monitor-${FLAVOR}-kafka-broker.default.svc.cluster.local:\${INTERNAL_PORT}" >> $TMPFILE
echo "    if [ \"\$ID\" == 0 ]" >> $TMPFILE
echo "    then" >> $TMPFILE
echo "     EXTERNAL=\${IP0}" >> $TMPFILE
echo "    fi" >> $TMPFILE
echo "    if [ \"\$ID\" == 1 ]" >> $TMPFILE
echo "    then" >> $TMPFILE
echo "     EXTERNAL=\${IP1}" >> $TMPFILE
echo "    fi" >> $TMPFILE
echo "    if [ \"\$ID\" == 2 ]" >> $TMPFILE
echo "    then" >> $TMPFILE
echo "     EXTERNAL=\${IP2}" >> $TMPFILE
echo "    fi" >> $TMPFILE
echo "    echo INTERNAL \"\$INTERNAL\"" >> $TMPFILE
echo "    echo EXTERNAL \"\$EXTERNAL\"" >> $TMPFILE
echo "    export KAFKA_CFG_ADVERTISED_LISTENERS=EXTERNAL_PLAINTEXT://\$EXTERNAL,INTERNAL_PLAINTEXT://\$INTERNAL" >> $TMPFILE
# Use below to turn off external access
#echo "    export KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL_PLAINTEXT://\$INTERNAL" >> $TMPFILE
echo "    export MY_BOOTSTRAP_SERVERS=\${IP0},\${IP1},\${IP2}" >> $TMPFILE
#
echo "    echo Set: KAFKA_CFG_BROKER_ID \"\$KAFKA_CFG_BROKER_ID\"" >> $TMPFILE
echo "    echo Set: KAFKA_CFG_LISTENERS \"\$KAFKA_CFG_LISTENERS\"" >> $TMPFILE
echo "    echo Set: KAFKA_CFG_ADVERTISED_LISTENERS \"\$KAFKA_CFG_ADVERTISED_LISTENERS\"" >> $TMPFILE
echo "    echo Set: MY_BOOTSTRAP_SERVERS \"\$MY_BOOTSTRAP_SERVERS\"" >> $TMPFILE
echo "---" >> $TMPFILE

cat $TMPFILE
echo ""
echo "kubectl delete -f $TMPFILE"
echo "kubectl create -f $TMPFILE && rm $TMPFILE"
echo ""
