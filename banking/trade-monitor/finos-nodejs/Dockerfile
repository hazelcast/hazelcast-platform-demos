FROM library/node:18.5.0

COPY target/classes/package.json ./
COPY target/classes/client.js ./

RUN npm install -g npm@8.12.1
RUN npm install hazelcast-client

COPY target/classes/custom-entrypoint.sh  /
ENTRYPOINT ["/bin/bash", "/custom-entrypoint.sh"]
