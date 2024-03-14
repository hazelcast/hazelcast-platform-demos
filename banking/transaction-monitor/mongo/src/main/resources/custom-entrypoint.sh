#!/bin/bash

if [ `echo ${MONGO_MAJOR} | cut -d. -f1` == "4" ]
then
 MONGOSH=mongo
else
 MONGOSH=mongosh
fi

# BACKGROUND SCRIPT, give Mongo some time to start then initiate CDC
(
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 1/2"
 sleep 20
 echo "$0: ::::::::::::::::::::::::::::::::::::::::::::::::::: 2/2"

 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 1/8"
 sleep 2
 echo "$0 : background script, ensure replica set (of one) is active"
 echo 'rs.initiate()'
 echo 'rs.initiate()' | $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 2/8"
 sleep 2
 echo "$0 : background script, reduce OpLog size to 990MB, minimum allowed"
 echo 'db.adminCommand({replSetResizeOplog: 1, size: 990})'
 echo 'db.adminCommand({replSetResizeOplog: 1, size: 990})' | $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 3/8"
 sleep 2
 echo "$0 : background script, confirm replica set (of one) is active"
 echo 'rs.status()'
 echo 'rs.status()' | $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 4/8"
 sleep 2
 echo "$0 : background script, confirm replica set (of one) configuration"
 echo 'rs.conf()'
 echo 'rs.conf()' | $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 5/8"
 sleep 2
 echo "$0 : background script, create collection"
 echo 'db.createCollection("externalControl", {})'
 $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
db.createCollection("externalControl",
{validator:
  {\$jsonSchema:{
     required: [ "jobName", "stateRequired" ],
     properties: {
        jobName: {
           bsonType: "string"
        },
        stateRequired: {
           bsonType: "string"
        }
     }
  }
})
show dbs
EOF
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 6/8"
 sleep 2
 echo "$0 : background script, insert first document"
 echo 'db.createCollection("externalControl", {})'
 $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
db.externalControl.insert( 
   [
     { _id: "01", jobName: "archiver", stateRequired: "SUSPENDED" }
   ] )   
EOF
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 7/8"
 sleep 2
 echo "$0 : background script, replication should be active"
 echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - 7/8"
 while [ true ]
 do
  echo ""
  date
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo "$0 : background script, check sizes every 60 seconds"
  $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
print("===================")
print("db.externalControl.count() - Size of the 'externalControl' collection in the '@my.other.admin.database@' database")
db.externalControl.count()
print("===================")
//print("db.oplog.rs.stats() - Status of the OpLog for the replica set (set of 1, DEV mode)")
//db.oplog.rs.stats()
//print("===================")
print("rs.printReplicationInfo() - Status of the OpLog for the replica set (set of 1, DEV mode)")
rs.printReplicationInfo()
print("===================")
EOF
  echo " "
  sleep 60
 done
  echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
  echo ""
) &

# Start Mongo
echo Starting Mongo
# Mongo 4.4 - /usr/local/bin/docker-entrypoint.sh
exec /usr/local/bin/docker-entrypoint.sh "$@"