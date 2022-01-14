#!/bin/bash

STATEFULSET_NAME=trade-monitor-kafka-broker
IPLIST=""
TMPFILE=/tmp/`basename $0`.$$

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
 echo `basename $0`: ERROR: Need 3 IPs in IP list: \"$IPLIST\"
 exit 1
fi

echo "---" >> $TMPFILE
echo "# ConfigMap for Kafka broker pods, each uses broker id to find IP in external IP list" >> $TMPFILE
echo "---" >> $TMPFILE
echo "apiVersion: v1" >> $TMPFILE
echo "kind: ConfigMap" >> $TMPFILE
echo "metadata:" >> $TMPFILE
echo "  name: trade-monitor-configmap" >> $TMPFILE
echo "data:" >> $TMPFILE
echo "  # Creates 'my-env.sh' mounted in /customize in pod" >> $TMPFILE
echo "  my-env.sh: |-" >> $TMPFILE
echo "    #!/bin/bash" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
echo "    echo Running setup.sh" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
# $ID should be numeric - 0, 1 or 2
echo "    ID=\`echo \$MY_POD_NAME | sed s/trade-monitor-kafka-broker-//\`" >> $TMPFILE
echo "    EXTERNAL_PORT=9092" >> $TMPFILE
echo "    INTERNAL_PORT=19092" >> $TMPFILE
echo "    echo ID \"\$ID\"" >> $TMPFILE
echo "    IPLIST=$IPLIST" >> $TMPFILE
echo "    echo IPLIST \"\$IPLIST\"" >> $TMPFILE
echo "    IP0=\`echo \$IPLIST | cut -d, -f1\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    IP1=\`echo \$IPLIST | cut -d, -f2\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    IP2=\`echo \$IPLIST | cut -d, -f3\`:\${EXTERNAL_PORT}" >> $TMPFILE
echo "    export KAFKA_CFG_BROKER_ID=\${ID}" >> $TMPFILE
echo "    INTERNAL=\${MY_POD_IP}:\${INTERNAL_PORT}" >> $TMPFILE
echo "    INTERNAL=\${MY_POD_NAME}.trade-monitor-kafka-broker.default.svc.cluster.local:\${INTERNAL_PORT}" >> $TMPFILE
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
echo "    export KAFKA_CFG_LISTENERS=EXTERNAL_PLAINTEXT://\$EXTERNAL,INTERNAL_PLAINTEXT://\$INTERNAL" >> $TMPFILE
# Use below to turn off external access
#echo "    export KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL_PLAINTEXT://\$INTERNAL" >> $TMPFILE
#echo "    export KAFKA_CFG_LISTENERS=INTERNAL_PLAINTEXT://\$INTERNAL" >> $TMPFILE
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
