FROM hazelcast/management-center:4.1-ALPHA-3

# Java 11
RUN java --version

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MC_LICENSE
ARG MC_ADMINUSER
ARG MC_ADMINPASSWORD

# To check health, uses port 8081 but internal URL. Preconfigure license, logon/password.
ENV JAVA_OPTS="-Dhazelcast.mc.healthCheck.enable=true \
 -DMC_ADMIN_USER=$MC_ADMINUSER \
 -DMC_ADMIN_PASSWORD=$MC_ADMINPASSWORD \
 -Dhazelcast.mc.license=$MC_LICENSE "

# To use the pre-configured data directory into the image, for config connections
COPY src/main/resources/data 	/data

CMD ["bash", "/mc-start.sh"]
