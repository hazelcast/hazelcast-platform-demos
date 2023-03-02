FROM --platform=linux/amd64 library/node:18.14

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@9.5.0
RUN npm install hazelcast-client

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
