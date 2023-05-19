FROM --platform=linux/amd64 library/node:20.1.0

ARG CONTROL_FILE

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@9.6.6
RUN npm install hazelcast-client

# Control file stops Docker copy failing if no PFX/PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pfx /tmp/
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
