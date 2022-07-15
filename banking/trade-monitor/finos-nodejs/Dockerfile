FROM library/node:18.5.0

ARG MC_CLUSTER1_NAME
ARG MC_CLUSTER1_ADDRESSLIST

# Preconfigure cluster connections
ENV MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME
ENV MC_CLUSTER1_ADDRESSLIST=$MC_CLUSTER1_ADDRESSLIST
ENV MY_KUBERNETES_ENABLED "true"

COPY target/classes/package.json ./
COPY target/classes/package-lock.json ./
COPY target/classes/public ./public
COPY target/classes/src ./src
COPY target/classes/tsconfig.json ./

RUN npm install -g npm@8.14.0
RUN npm install

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
