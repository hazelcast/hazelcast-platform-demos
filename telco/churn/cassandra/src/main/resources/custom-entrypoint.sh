#!/bin/bash

# Enable CDC and other config
mv $CASSANDRA_HOME/conf/cassandra.yaml $CASSANDRA_HOME/conf/cassandra.yaml.orig
cat $CASSANDRA_HOME/conf/cassandra.yaml.orig \
 | sed 's/cdc_enabled: false/cdc_enabled: true/' \
 | grep -v '^enable_sasi_indexes'  > $CASSANDRA_HOME/conf/cassandra.yaml
mkdir -p $CASSANDRA_HOME/data/commitlog/archive
mkdir -p $CASSANDRA_HOME/data/commitlog/error
mkdir -p $CASSANDRA_HOME/data/commitlog/relocation
chown -R cassandra $CASSANDRA_HOME
mv /debezium-connector-cassandra/debezium-connector-cassandra.conf /debezium-connector-cassandra/debezium-connector-cassandra.conf.orig
mkdir /debezium-connector-cassandra/offset_dir
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
  `grep ^cdc_enabled $CASSANDRA_HOME/conf/cassandra.yaml` " " \
  `grep ^kafka.producer.bootstrap.servers /debezium-connector-cassandra/debezium-connector-cassandra.conf`
 echo "$0: ========================================================"

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

 # Start CDC, with Kafka server addresses from environment
 (cd /debezium-connector-cassandra ;java -Dcassandra.storagedir=$CASSANDRA_HOME/data -jar debezium-connector-cassandra.jar debezium-connector-cassandra.conf) &

 #
 while `cqlsh -e exit 2> /dev/null` 
 do 
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, temp code, flush CDC to disk"
  echo nodetool flush churn cdr
  nodetool flush churn cdr
  echo ls -l $CASSANDRA_HOME/data/cdc_raw
  ls -l $CASSANDRA_HOME/data/cdc_raw
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  sleep 10;
 done
) &

# Start Cassandra
exec /docker-entrypoint.sh "$@"
