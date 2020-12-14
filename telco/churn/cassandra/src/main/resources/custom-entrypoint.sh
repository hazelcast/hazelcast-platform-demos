#!/bin/bash

# Cassandra 3.11.4, set auto for 3.11.9
CASSANDRA_CONF=/etc/cassandra
CASSANDRA_HOME=/var/lib/cassandra

# Enable CDC and other config. See also https://issues.apache.org/jira/browse/CASSANDRA-12148
mv $CASSANDRA_CONF/cassandra.yaml $CASSANDRA_CONF/cassandra.yaml.orig
cat $CASSANDRA_CONF/cassandra.yaml.orig \
 | sed 's/cdc_enabled: false/cdc_enabled: true/' \
 | sed 's/commitlog_segment_size_in_mb: 32/commitlog_segment_size_in_mb: 1/' \
 | grep -v ^replica_filtering_protection \
 | grep -v cached_rows_warn_threshold \
 | grep -v cached_rows_fail_threshold \
 | grep -v ^enable_sasi_indexes  > $CASSANDRA_CONF/cassandra.yaml
echo commitlog_total_space_in_mb: 2 >> $CASSANDRA_CONF/cassandra.yaml
#echo cdc_total_space_in_mb: 256 >> $CASSANDRA_CONF/cassandra.yaml
mkdir -p $CASSANDRA_HOME/data/commitlog/relocation/archive
mkdir -p $CASSANDRA_HOME/data/commitlog/relocation/error
chown -R cassandra $CASSANDRA_HOME
mv /debezium-connector-cassandra/debezium-connector-cassandra.conf /debezium-connector-cassandra/debezium-connector-cassandra.conf.orig
mkdir -p /debezium-connector-cassandra/offset_dir
cat /debezium-connector-cassandra/debezium-connector-cassandra.conf.orig \
 | sed s/@MY_BOOTSTRAP_SERVERS@/$MY_BOOTSTRAP_SERVERS/ \
 > /debezium-connector-cassandra/debezium-connector-cassandra.conf

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
 echo "$0 : background script, believe Cassandra to be useable," \
  `grep ^cdc_enabled $CASSANDRA_CONF/cassandra.yaml` " " \
  `grep ^kafka.producer.bootstrap.servers /debezium-connector-cassandra/debezium-connector-cassandra.conf`
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

 # Start CDC, with Kafka server addresses from environment. Container probably Java 8
 (cd /debezium-connector-cassandra ;java -Dlog4j.configurationFile=./log4j.properties -Dcassandra.storagedir=$CASSANDRA_HOME -jar debezium-connector-cassandra.jar debezium-connector-cassandra.conf) &
 #
 #
 while true 
 do 
  sleep 15;
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, confirm CDC to disk"
  # Presence of this directory is proof of CDC is enabled at node level.
  if [ ! -d $CASSANDRA_HOME/cdc_raw ]
  then
   echo "$0 : "$CASSANDRA_HOME/cdc_raw MISSING
  fi
  # If Debezium runs, it's filewatcher should empty this directory
  # Should be two commit logs, given log size and total size configured above
  echo "$0 : "ls -l \$CASSANDRA_HOME/\*/CommitLog\*
  ls -l $CASSANDRA_HOME/*/CommitLog*
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
 done
) &

# Start Cassandra
echo Starting Cassandra
sleep 2
# Cassandra 3 - /docker-entrypoint.sh
# Cassandra 4 - /usr/local/bin/docker-entrypoint.sh
exec /docker-entrypoint.sh "$@"
