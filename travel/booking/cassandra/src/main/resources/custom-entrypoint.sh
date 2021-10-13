#!/bin/bash

CASSANDRA_CONF=/etc/cassandra
CASSANDRA_HOME=/opt/cassandra

# BACKGROUND SCRIPT, will probe Cassandra until it's ready. Not clean, but seems to be the standard approach
(
 until `cqlsh -e exit 2> /dev/null` 
 do 
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, waiting for Cassandra to come online"
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  sleep 5;
 done

 sleep 3

 # Run all CQL files in directory
 DIR=/cql
 ls $DIR | while read -r ALINE
 do
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, apply $ALINE"
  cqlsh -f $DIR/$ALINE
  RC=$?
  echo "$0 : background script, applied $ALINE, rc=$RC"
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 done

 sleep 3

) &

# Start Cassandra
echo Starting Cassandra
sleep 2
# Cassandra 3 - /docker-entrypoint.sh
# Cassandra 4 - /usr/local/bin/docker-entrypoint.sh
exec /usr/local/bin/docker-entrypoint.sh "$@"
