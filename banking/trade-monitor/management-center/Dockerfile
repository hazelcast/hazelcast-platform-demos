FROM hazelcast/management-center:5.0-BETA-2

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MC_LICENSE
ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# To check health, uses port 8081 but internal URL. Preconfigure license, logon/password.
# -Dhazelcast.mc.periodic.healthcheck.enabled=false \
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.prometheusExporter.enabled=true \
 -Dhazelcast.mc.license=$MC_LICENSE"

# Used by /mc-start.sh to create admin user
ENV MC_ADMIN_USER=$MY_ADMINUSER
ENV MC_ADMIN_PASSWORD=$MY_ADMINPASSWORD

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST

# If want to query custom classes
ARG JAR_FILE
COPY target/${JAR_FILE} /application.jar
ENV MC_CLASSPATH=/application.jar

# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_ADDRESSLIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_ADDRESSLIST} \
      && echo bin/mc-start.sh \
      && bin/mc-start.sh \
     "]
