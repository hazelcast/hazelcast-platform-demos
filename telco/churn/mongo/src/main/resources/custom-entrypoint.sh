#!/bin/bash

# BACKGROUND SCRIPT, give Mongo some time to start then initiate CDC
(
 sleep 3
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"
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
) &

# Start Mongo
echo Starting Mongo
# Mongo 4.4 - /usr/local/bin/docker-entrypoint.sh
exec /usr/local/bin/docker-entrypoint.sh "$@"
