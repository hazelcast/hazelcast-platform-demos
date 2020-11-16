#!/bin/bash

# BACKGROUND SCRIPT, give Mongo some time to start then initiate CDC
(
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"
 echo "$0 : background script, start mongos"
 echo TODO TODO TODO
 echo TODO mongos --configdb churn/mongo:27019
 echo TODO TODO TODO
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"

 # Replica set name "churn" set in Dockerfile
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, ensure replica set (of one) is active"
 echo 'rs.initiate()' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, confirm replica set (of one) is active"
 echo 'rs.status()' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, create collection"
 echo 'db.createCollection("customer", {})' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 echo "$0 : background script, shard collection"
 echo 'sh.shardCollection("admin.customer", {_id:1})' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
) &

# Start Mongo
echo Starting Mongo
# Mongo 4.2 - /usr/local/bin/docker-entrypoint.sh
exec /usr/local/bin/docker-entrypoint.sh "$@"
