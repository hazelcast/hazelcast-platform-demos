#!/bin/bash

# BACKGROUND SCRIPT, will probe Cassandra until it's ready. Not clean, but seems to be the standard approach
(
 until `cqlsh -e exit 2> /dev/null` 
 do 
  echo "$0 : background script, waiting for Cassandra to come online"
  sleep 5;
 done

 echo "$0 : background script, believe Cassandra to be useable"

 # Run all CQL files in directory
 DIR=/cql
 ls $DIR | while read -r ALINE
 do
  echo "$0 : background script, apply $ALINE"
  cqlsh -f $DIR/$ALINE
  RC=$?
  echo "$0 : background script, applied $ALINE, rc=$RC"
 done
) &

# Start Cassandra
exec /docker-entrypoint.sh "$@"
