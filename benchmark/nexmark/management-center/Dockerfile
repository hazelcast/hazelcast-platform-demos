#FROM --platform=linux/amd64 hazelcast/management-center:latest-snapshot
FROM --platform=linux/amd64 hazelcast/management-center:5.7.0

# From pom.xml
ARG MC_LICENSE
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# Connection to cluster1
COPY target/classes/hazelcast-client-cluster1.xml /

# Any Java config for MC.
ENV JAVA_HEAP_SIZE="12g"
ENV JAVA_OPTS="\
 -Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.license=$MC_LICENSE \
 -Dhazelcast.mc.prometheusExporter.enabled=true"
 
# Used by /start.sh to create admin user unless MC_INIT_CMD used
ENV MC_ADMIN_USER=$MY_ADMINUSER
ENV MC_ADMIN_PASSWORD=$MY_ADMINPASSWORD 
 
# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && export JAVA_OPTS=\"-Xmx${JAVA_HEAP_SIZE} -Xms${JAVA_HEAP_SIZE} ${JAVA_OPTS}\" \
      && echo JAVA_OPTS=${JAVA_OPTS} \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --client-config=/hazelcast-client-cluster1.xml \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --client-config=/hazelcast-client-cluster1.xml \      
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/hz-mc start \
      && bin/hz-mc start \
     "]
