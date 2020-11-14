#!/bin/bash

# BACKGROUND SCRIPT, allow process to start then inject JSON config by REST to start connector
(
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"

 # Image already has classes, just needs config to start connector
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, initiate Mongo connector via REST"
 echo "$0 : BEFORE"
 echo curl http://localhost:8083/connectors
 curl http://localhost:8083/connectors
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo curl -X POST -H 'Content-Type: application/json' --data @/mongo.json http://localhost:8083/connectors
 curl -X POST -H 'Content-Type: application/json' --data @/mongo.json http://localhost:8083/connectors
 RC=$?
 if [ $RC -ne 0 ]
 then
  echo ===
  echo RC=${RC}
  echo ===
 fi
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : AFTER"
 echo curl http://localhost:8083/connectors
 curl http://localhost:8083/connectors
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
) &

# Start main image process for Kafka Connect
echo Starting Kafka Connect
exec /docker-entrypoint.sh "$@"
