FROM --platform=linux/amd64 library/node:22.11.0

ARG CONTROL_FILE

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@10.9.1
RUN npm install hazelcast-client

# Control file stops Docker copy failing if no PFX/PEM files found. Put in target by maven-antrun-plugin
COPY target/classes/${CONTROL_FILE} target/*.pfx /tmp/
COPY target/classes/${CONTROL_FILE} target/*.pem /tmp/

# Default assume in Kubernetes, else Docker
ENV HOST_IP=""
ENV MY_KUBERNETES_ENABLED="true"

# Use "touch" to ensure *.pem files exist even if not used
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo date \
      && date \
      && echo @@@@@@@@@@ \
      && echo touch /tmp/ca.pem /tmp/cert.pem /tmp/key.pem \
      && touch /tmp/ca.pem /tmp/cert.pem /tmp/key.pem \
      && echo ls -l /tmp \
      && ls -l /tmp \
      && echo @@@@@@@@@@ \
      && echo npm --loglevel=error start \
      && echo npm --loglevel=error start \
      && npm --loglevel=error start \
     "]
