FROM hazelcast/management-center:latest-snapshot

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MC_LICENSE
ARG MC_ADMINUSER
ARG MC_ADMINPASSWORD
ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST
ARG MC_CLUSTER2_NAME
ARG MC_CLUSTER2_ADDRESSLIST

# To check health, uses port 8081 but internal URL. Preconfigure license, logon/password.
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.prometheusExporter.enabled=true \
 -Dhazelcast.mc.license=$MC_LICENSE \
 -Xmx3G -Xms3G \
 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/neil"

# Used by /mc-start.sh to create admin user
ENV MC_ADMIN_USER=$MC_ADMINUSER
ENV MC_ADMIN_PASSWORD=$MC_ADMINPASSWORD

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MC_CLUSTER2_NAME=$MC_CLUSTER2_NAME
ENV MC_CLUSTER2_ADDRESSLIST=$MC_CLUSTER2_ADDRESSLIST

# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_ADDRESSLIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER1_NAME} --member-addresses=${MC_CLUSTER1_ADDRESSLIST} \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_ADDRESSLIST} \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --cluster-name=${MC_CLUSTER2_NAME} --member-addresses=${MC_CLUSTER2_ADDRESSLIST} \
      && bin/mc-start.sh \
     "]
