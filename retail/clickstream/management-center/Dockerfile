#FROM hazelcast/management-center:latest-snapshot
FROM hazelcast/management-center:5.1.1

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MC_LICENSE
ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST
ARG MC_CLUSTER1_PORTLIST
ARG MC_CLUSTER2_NAME
ARG MC_CLUSTER2_ADDRESSLIST
ARG MC_CLUSTER2_PORTLIST
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# To check health, uses port 8081 but internal URL. Preconfigure license, logon/password.
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.prometheusExporter.enabled=true \
 -Dhazelcast.mc.license=$MC_LICENSE \
 -Dhazelcast.mc.rest.enabled=true \
 -Xmx3G -Xms3G"

# Used by /mc-start.sh to create admin user unless MC_INIT_CMD used
#ENV MC_ADMIN_USER=$MY_ADMINUSER
#ENV MC_ADMIN_PASSWORD=$MY_ADMINPASSWORD

# Preconfigure cluster connections
ENV HOST_IP ""
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MC_CLUSTER1_PORTLIST=$MC_CLUSTER1_PORTLIST
ENV MC_CLUSTER2_NAME=$MC_CLUSTER2_NAME
ENV MC_CLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST
ENV MC_CLUSTER2_PORTLIST=$MC_CLUSTER2_PORTLIST
# Set security provider ("admin" user with role "admin") and request bearer token before MC comes online.
# If removed need MC_ADMIN_USER and MC_ADMIN_PASSWORD env variables set for auto-configuration.
ENV MC_INIT_CMD="./bin/mc-conf.sh user create -n=$MY_ADMINUSER -p=$MY_ADMINPASSWORD -r=admin && ./bin/mc-conf.sh user issue-token -H=/data --username=$MY_ADMINUSER"

# If want to query custom classes
#ARG JAR_FILE
#COPY target/${JAR_FILE} /application.jar
#ENV MC_CLASSPATH=/application.jar

# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo Input \
#      && echo MC_ADMIN_USER: $MC_ADMIN_USER \
#      && echo MC_ADMIN_PASSWORD: $MC_ADMIN_PASSWORD \
      && echo MC_CLUSTER1_NAME: $MC_CLUSTER1_NAME \
      && echo MC_CLUSTER1_ADDRESSLIST: $MC_CLUSTER1_ADDRESSLIST \
      && echo MC_CLUSTER1_PORTLIST: $MC_CLUSTER1_PORTLIST \
      && echo MC_CLUSTER2_NAME: $MC_CLUSTER2_NAME \
      && echo MC_CLUSTER2_ADDRESSLIST: $MC_CLUSTER2_ADDRESSLIST \
      && echo MC_CLUSTER2_PORTLIST: $MC_CLUSTER2_PORTLIST \
      && echo MC_INIT_CMD: $MC_INIT_CMD \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo Derived \
      && MC_EMPTY='' \
      && MC_CLUSTER1_DOCKERLIST=${HOST_IP}:`echo $MC_CLUSTER1_PORTLIST | sed s/,/,${HOST_IP}:/g` \
      && MC_CLUSTER2_DOCKERLIST=${HOST_IP}:`echo $MC_CLUSTER2_PORTLIST | sed s/,/,${HOST_IP}:/g` \
      # Conditional free, take Docker list if HOST_IP set.
      && MC_CLUSTER1_LIST=`echo ${HOST_IP:-${MC_EMPTY}} $MC_CLUSTER1_DOCKERLIST $MC_CLUSTER1_ADDRESSLIST |awk '{print $2}'` \
      && MC_CLUSTER2_LIST=`echo ${HOST_IP:-${MC_EMPTY}} $MC_CLUSTER2_DOCKERLIST $MC_CLUSTER2_ADDRESSLIST |awk '{print $2}'` \
      && echo MC_CLUSTER1_LIST: ${MC_CLUSTER1_LIST} \
      && echo MC_CLUSTER2_LIST: ${MC_CLUSTER2_LIST} \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_LIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_LIST} \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_LIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_LIST} \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/hz-mc start \
      && bin/hz-mc start \
     "]
