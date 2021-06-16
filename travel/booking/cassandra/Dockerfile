FROM cassandra:4.0

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# ENV uses ARG
ENV CASSANDRA_USER=$MY_OTHERUSER
ENV CASSANDRA_PASSWORD=$MY_OTHERPASSWORD
ENV CASSANDRA_CLUSTER_NAME=$MY_OTHERDATABASE

# Setup
COPY target/classes/cql  /cql
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh
ENTRYPOINT ["/custom-entrypoint.sh"]