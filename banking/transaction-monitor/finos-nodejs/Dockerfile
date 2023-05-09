FROM --platform=linux/amd64 library/node:20.1.0

ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MY_KUBERNETES_ENABLED "true"

COPY target/classes/package.json ./
COPY target/classes/public ./public
COPY target/classes/src ./src
COPY target/classes/tsconfig.json ./
COPY target/classes/webpack.config.js ./

RUN npm install -g npm@9.6.6
RUN npm install -g hazelcast-client --save
RUN npm install -g fs
RUN npm install

COPY target/classes/custom-entrypoint.sh  /
CMD ["bash", "-c", "set -euo pipefail \
      && echo @@@@@@@@@@ \
      && echo arch \
      && arch \
      && echo @@@@@@@@@@ \
      && chmod 755 /custom-entrypoint.sh \
      && echo /custom-entrypoint.sh \
      && /custom-entrypoint.sh \
     "]
