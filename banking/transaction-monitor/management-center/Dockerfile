FROM --platform=linux/amd64 hazelcast/management-center:latest-snapshot
#FROM --platform=linux/amd64 hazelcast/management-center:5.6.0

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MC_LICENSE
ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST
ARG MC_CLUSTER2_NAME
ARG MC_CLUSTER2_ADDRESSLIST
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# To check health, uses port 8081 but internal URL. Preconfigure license, logon/password.
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.prometheusExporter.enabled=true \
 -Dhazelcast.mc.websocket.buffer-size=20MB \
 -Dhazelcast.mc.license=$MC_LICENSE"

# Used by /mc-start.sh to create admin user
ENV MC_ADMIN_USER=$MY_ADMINUSER
ENV MC_ADMIN_PASSWORD=$MY_ADMINPASSWORD

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MC_CLUSTER2_NAME=$MC_CLUSTER2_NAME
ENV MC_CLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST
ENV MC_EMPTY=""

# If want to query custom classes
ARG JAR_FILE
COPY target/${JAR_FILE} /application.jar
ENV MC_CLASSPATH=/application.jar

# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo java --version \
      && java --version \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && MC_CLUSTER1_LIST=`echo ${MC_CLUSTER1_ADDRESSLIST_OVERRIDE:-${MC_EMPTY}} $MC_CLUSTER1_ADDRESSLIST |awk '{print $1}'` \
      && echo MC_CLUSTER1_NAME: ${MC_CLUSTER1_NAME} \
      && echo MC_CLUSTER1_LIST: ${MC_CLUSTER1_LIST} \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_LIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_LIST} \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && MC_CLUSTER2_LIST=`echo ${MC_CLUSTER2_ADDRESSLIST_OVERRIDE:-${MC_EMPTY}} $MC_CLUSTER2_ADDRESSLIST |awk '{print $1}'` \
      && echo MC_CLUSTER2_NAME: ${MC_CLUSTER2_NAME} \
      && echo MC_CLUSTER2_LIST: ${MC_CLUSTER2_LIST} \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_LIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_LIST} \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && echo bin/hz-mc start \
      && bin/hz-mc start \
     "]
