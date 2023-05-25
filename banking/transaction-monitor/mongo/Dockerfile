# Need AVX on x86_64 for Mongo 5 upwards
#FROM --platform=linux/amd64 mongo:6.0.6
FROM --platform=linux/amd64 mongo:4.2.0

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG FLAVOR
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV FLAVOR=${FLAVOR}
ENV MONGO_INITDB_ROOT_USERNAME=$MY_OTHERUSER
ENV MONGO_INITDB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MONGO_INITDB_DATABASE=$MY_OTHERDATABASE

# Config
COPY target/classes/mongod.conf /etc
COPY target/classes/roles.sh /docker-entrypoint-initdb.d/
RUN chmod 755 /docker-entrypoint-initdb.d/roles.sh

# Override /usr/local/bin/docker-
COPY target/classes/custom-entrypoint.sh  /
RUN chmod 755 /custom-entrypoint.sh
#ENTRYPOINT ["/custom-entrypoint.sh" ]
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo /custom-entrypoint.sh \
      && /custom-entrypoint.sh \
     "]