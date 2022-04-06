FROM hazelcast/management-center:latest-snapshot

# From pom.xml
ARG MC_LICENSE
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# Connection to cluster1
COPY target/classes/hazelcast-client-cluster1.xml /

# Any Java config for MC. Note "hazelcast.mc.metrics.consumer.large.cluster.mode"
ENV JAVA_HEAP_SIZE="11g"
ENV JAVA_OPTS="\
 -Dhazelcast.mc.healthCheck.enable=true \
 -Dhazelcast.mc.license=$MC_LICENSE \
 -Dhazelcast.mc.metrics.consumer.large.cluster.mode=true \
 -Dhazelcast.mc.prometheusExporter.enabled=true \
 -Xms$JAVA_HEAP_SIZE \
 -Xmx$JAVA_HEAP_SIZE"
 
# Used by /start.sh to create admin user unless MC_INIT_CMD used
ENV MC_ADMIN_USER=$MY_ADMINUSER
ENV MC_ADMIN_PASSWORD=$MY_ADMINPASSWORD 
 
# Start Management Center
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/mc-conf.sh cluster add -H=${MC_DATA} --client-config=/hazelcast-client-cluster1.xml \
      && bin/mc-conf.sh cluster add -H=${MC_DATA} --client-config=/hazelcast-client-cluster1.xml \      
      && echo @@@@@@@@@@@@@@@@@@@@ \
      && echo bin/hz-mc start \
      && bin/hz-mc start \
     "]
