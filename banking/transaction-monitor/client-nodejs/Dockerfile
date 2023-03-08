FROM --platform=linux/amd64 library/node:18.14

ARG CONTROL_FILE

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@9.5.0
RUN npm install hazelcast-client

# Control file stops Docker copy failing if no PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pem /tmp/

# Default assume in Kubernetes, else Docker
ENV HOST_IP ""
ENV MY_KUBERNETES_ENABLED "true"

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && echo npm --loglevel=error start \
      && npm --loglevel=error start \
     "]
