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

STATEFULSET_NAME=transaction-monitor-${FLAVOR}-kafka-broker
POSTGRES_NAME=transaction-monitor-${FLAVOR}-postgres
PULSAR_NAME=transaction-monitor-${FLAVOR}-pulsar
IPLIST=""
POSTGRESADDRESS=""
PULSARLIST=""
TMPFILE=/tmp/`basename $0`.$$

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

# One POSTGRES
SVC=${POSTGRES_NAME}
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
  POSTGRESADDRESS="$IP"
 fi
fi

# One Pulsar
SVC=${PULSAR_NAME}
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
  PULSARLIST="$IP"
 fi
fi

IPCOUNT=`echo $IPLIST | sed 's/,/ /g' | wc -w`
if [ $IPCOUNT -ne 3 ]
then
 echo `basename $0`: ERROR: Need 3 IPs in Kafka IP list: \"$IPLIST\"
 exit 1
fi
POSTGRESCOUNT=`echo $POSTGRESADDRESS | sed 's/,/ /g' | wc -w`
if [ $POSTGRESCOUNT -ne 1 ]
then
 echo `basename $0`: ERROR: Need 1 IPs in Postgres IP list: \"$POSTGRESADDRES\"
 exit 1
fi
PULSARCOUNT=`echo $PULSARLIST | sed 's/,/ /g' | wc -w`
if [ $PULSARCOUNT -ne 1 ]
then
 echo `basename $0`: ERROR: Need 1 IPs in Pulsar IP list: \"$PULSARLIST\"
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
echo "    POSTGRESADDRESS=$POSTGRESADDRESS" >> $TMPFILE
echo "    echo POSSTGRESADDRESS \"\$POSTGRESADDRESS\"" >> $TMPFILE
echo "    PULSARLIST=$PULSARLIST" >> $TMPFILE
echo "    echo PULSARLIST \"\$PULSARLIST\"" >> $TMPFILE
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
echo "    export MY_POSTGRES_ADDRESS=\${POSTGRESADDRESS}" >> $TMPFILE
echo "    export MY_PULSAR_LIST=\${PULSARLIST}" >> $TMPFILE
#
echo "    echo Set: KAFKA_CFG_BROKER_ID \"\$KAFKA_CFG_BROKER_ID\"" >> $TMPFILE
echo "    echo Set: KAFKA_CFG_LISTENERS \"\$KAFKA_CFG_LISTENERS\"" >> $TMPFILE
echo "    echo Set: KAFKA_CFG_ADVERTISED_LISTENERS \"\$KAFKA_CFG_ADVERTISED_LISTENERS\"" >> $TMPFILE
echo "    echo Set: MY_BOOTSTRAP_SERVERS \"\$MY_BOOTSTRAP_SERVERS\"" >> $TMPFILE
echo "    echo Set: MY_PULSAR_LIST \"\$MY_PULSAR_LIST\"" >> $TMPFILE
echo "---" >> $TMPFILE

cat $TMPFILE
echo ""
echo "kubectl delete -f $TMPFILE"
echo "kubectl create -f $TMPFILE && rm $TMPFILE"
echo ""
