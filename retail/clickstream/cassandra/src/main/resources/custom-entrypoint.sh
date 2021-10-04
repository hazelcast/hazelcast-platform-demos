#!/bin/bash

# Cassandra 3.11.4, set auto for 3.11.9
CASSANDRA_CONF=/etc/cassandra
CASSANDRA_HOME=/var/lib/cassandra

# Config.
# NOTE CDC REQUIRES A SMALL commitlog_segment_size_in_mb
# BUT DEFAULT commitlog_segment_size_in_mb 32 IS BETTER FOR SAVING MODELS

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
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::::::::"
 nodetool status
 echo "$0: ========================================================"
 echo "$0 : background script, believe Cassandra to be useable"
 echo "$0: ========================================================"

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
exec /docker-entrypoint.sh "$@"
