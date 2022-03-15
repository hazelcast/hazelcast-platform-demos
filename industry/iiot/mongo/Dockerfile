FROM mongo:5.0.5

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_OTHERUSER
ARG MY_OTHERPASSWORD
ARG MY_OTHERDATABASE

# Environment variables for shell user
ENV MONGO_INITDB_ROOT_USERNAME=$MY_OTHERUSER
ENV MONGO_INITDB_ROOT_PASSWORD=$MY_OTHERPASSWORD
ENV MONGO_INITDB_DATABASE=$MY_OTHERDATABASE

# "Options set by command line","attr":{"options":{"net":{"bindIp":"*"}}}}
RUN sed 's/bindIp: 127.0.0.1/bindIp: 0.0.0.0/' < /etc/mongod.conf.orig > /tmp/mongod.conf
RUN cp /tmp/mongod.conf /etc/mongod.conf

# Helpful directories
RUN mkdir -p /var/lib/mongodb/
RUN chmod 755 /var/lib/mongodb/
RUN mkdir /home/mongodb
RUN chmod 777 /home/mongodb
RUN touch /home/mongodb/.dbshell
RUN chmod 777 /home/mongodb/.dbshell

# Initialization scripts, run by Mongo in sequence
COPY target/classes/*.sh /docker-entrypoint-initdb.d/
RUN chmod 755 /docker-entrypoint-initdb.d/*.sh
