FROM library/node:16.5.0

COPY target/classes/package.json ./
COPY target/classes/flight.js ./

RUN npm install -g npm@7.19.1
RUN npm install hazelcast-client

CMD [ "npm", "--loglevel=error", "start" ]
