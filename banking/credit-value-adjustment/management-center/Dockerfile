FROM hazelcast/management-center:4.1-ALPHA-3

# Java 11
RUN java --version

# To check health, uses port 8081 but internal URL. Preconfigure license
ARG MC_LICENSE
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true -Dhazelcast.mc.license=$MC_LICENSE"

CMD ["bash", "/mc-start.sh"]
