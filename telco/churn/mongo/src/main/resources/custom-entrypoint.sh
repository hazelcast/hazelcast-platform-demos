#!/bin/bash

# BACKGROUND SCRIPT, give Mongo some time to start then initiate CDC
(
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 1/2"
 sleep 10
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 2/2"

 # Replica set name "churn" set in Dockerfile
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 1/7"
 sleep 10
 echo "$0 : background script, ensure replica set (of one) is active"
 echo 'rs.initiate()'
 echo 'rs.initiate()' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 2/7"
 sleep 10
 echo "$0 : background script, reduce OpLog size to 990MB, minimum allowed"
 echo 'db.adminCommand({replSetResizeOplog: 1, size: 990})'
 echo 'db.adminCommand({replSetResizeOplog: 1, size: 990})' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 3/7"
 sleep 10
 echo "$0 : background script, confirm replica set (of one) is active"
 echo 'rs.status()'
 echo 'rs.status()' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 4/7"
 sleep 10
 echo "$0 : background script, confirm replica set (of one) configuration"
 echo 'rs.conf()'
 echo 'rs.conf()' | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 5/7"
 echo "$0 : background script, create collection"
 echo 'db.createCollection("customer", {})'
mongo -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use churn
db.createCollection("customer", {})
show dbs
EOF
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 6/7"
 sleep 10
 echo "$0 : background script, replication should be active"
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 7/7"
 while [ true ]
 do
  echo ""
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, check sizes every 15 seconds"
  mongo -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use churn
print("===================")
print("db.customer.count() - Size of the 'customer' collection in the 'churn' database")
db.customer.count()
print("===================")
//print("db.oplog.rs.stats() - Status of the OpLog for the replica set (set of 1, DEV mode)")
//db.oplog.rs.stats()
//print("===================")
print("rs.printReplicationInfo() - Status of the OpLog for the replica set (set of 1, DEV mode)")
rs.printReplicationInfo()
print("===================")
EOF
  echo " "
  sleep 15
 done
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo ""
) &

# Start Mongo
echo Starting Mongo
# Mongo 4.2 - /usr/local/bin/docker-entrypoint.sh
exec /usr/local/bin/docker-entrypoint.sh "$@"
