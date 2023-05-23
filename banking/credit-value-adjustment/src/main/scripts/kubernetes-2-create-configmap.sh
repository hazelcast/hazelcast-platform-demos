#!/bin/bash

CPP_NAME=cva-cpp
EXPECTED_PREDECESSOR=kubernetes-cpp.yaml
IPLIST=""
CPPADDRESS=""
TMPFILE=/tmp/`basename $0`.$$

# Find Load Balancer
SVC=${CPP_NAME}
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
  echo Was $EXPECTED_PREDECESSOR run\?
 else
  echo Try again in 30 seconds\? '<pending>'/'<none>' will clear.
 fi
 echo ""
else
 IP=`kubectl get svc $SVC -o jsonpath='{.status.loadBalancer.ingress[0].ip}'`
 if [ "$IP" != "" ]
 then
  CPPADDRESS="$IP"
 fi
fi

CPPCOUNT=`echo $CPPADDRESS | sed 's/,/ /g' | wc -w`
if [ $CPPCOUNT -ne 1 ]
then
 echo `basename $0`: ERROR: Need 1 IPs in C++ IP list: \"$CPPADDRESS\"
 exit 1
fi

echo "---" >> $TMPFILE
echo "# ConfigMap for Kafka broker pods, each uses broker id to find IP in external IP list" >> $TMPFILE
echo "---" >> $TMPFILE
echo "apiVersion: v1" >> $TMPFILE
echo "kind: ConfigMap" >> $TMPFILE
echo "metadata:" >> $TMPFILE
echo "  name: cva-configmap" >> $TMPFILE
echo "data:" >> $TMPFILE
echo "  # Creates 'my-env.sh' mounted in /customize in pod" >> $TMPFILE
echo "  my-env.sh: |-" >> $TMPFILE
echo "    #!/bin/bash" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
echo "    echo Running setup.sh" >> $TMPFILE
echo "    echo @@@@@@@@@@@@@@@@" >> $TMPFILE
echo "    CPPADDRESS=$CPPADDRESS" >> $TMPFILE
echo "    echo CPPADDRESS \"\$CPPADDRESS\"" >> $TMPFILE
echo "    export MY_CPP_SERVICE=\${CPPADDRESS}" >> $TMPFILE
#
echo "    echo Set: MY_CPP_SERVICE \"\$MY_CPP_SERVICE\"" >> $TMPFILE
echo "---" >> $TMPFILE

cat $TMPFILE
echo ""
echo "kubectl delete -f $TMPFILE"
echo "kubectl create -f $TMPFILE && rm $TMPFILE"
echo ""
