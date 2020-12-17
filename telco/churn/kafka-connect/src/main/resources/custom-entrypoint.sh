#!/bin/bash

# BACKGROUND SCRIPT, allow process to start then inject JSON config by REST to start connector
(
 sleep 10
 # Image already has classes, just needs config to start connector
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, initiate Mongo connector via REST"
 echo "$0 : BEFORE"
 echo curl http://localhost:8083/connectors
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 # Stashed in Kafka config topic, don't want two, so delete for redeploy
 echo "$0 : background script, delete any residual Mongo connector via REST"
 echo curl -X DELETE http://localhost:8083/connectors/debezium-connector-mongodb
 curl -X DELETE http://localhost:8083/connectors/debezium-connector-mongodb

 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 1/4"
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 2/4"
 echo BOOTSTRAP_SERVERS=$BOOTSTRAP_SERVERS
 echo MY_MONGO_LOCATION=$MY_MONGO_LOCATION
 sed s/MY_MONGO_LOCATION/$MY_MONGO_LOCATION/ < $KAFKA_HOME/config/mongo.json > $KAFKA_HOME/config/mongo.json.$$
 mv $KAFKA_HOME/config/mongo.json.$$ $KAFKA_HOME/config/mongo.json
 echo egrep 'mongodb.hosts|mongodb.name|mongodb.user' $KAFKA_HOME/config/mongo.json
 egrep 'mongodb.hosts|mongodb.name|mongodb.user' $KAFKA_HOME/config/mongo.json
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 3/4"
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 4/4"

 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, initiate Mongo connector via REST"
 echo curl -X POST -H 'Content-Type: application/json' --data @$KAFKA_HOME/config/mongo.json http://localhost:8083/connectors
 curl -X POST -H 'Content-Type: application/json' --data @$KAFKA_HOME/config/mongo.json http://localhost:8083/connectors
 RC=$?
 if [ $RC -ne 0 ]
 then
  echo ===
  echo RC=${RC}
  echo ===
 fi
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, initiate Mongo connector via REST"
 echo "$0 : AFTER"
 echo curl http://localhost:8083/connectors
 curl http://localhost:8083/connectors
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
) &

# Start main image process for Kafka Connect
echo Starting Kafka Connect
exec /docker-entrypoint.sh "$@"
