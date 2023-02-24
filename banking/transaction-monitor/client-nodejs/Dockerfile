FROM --platform=linux/amd64 library/node:18.14

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@9.5.0
RUN npm install hazelcast-client

CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && echo npm --loglevel=error start \
      && npm --loglevel=error start \
     "]