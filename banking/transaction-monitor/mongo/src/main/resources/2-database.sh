# Script is sourced, can't use $0
MYNAME=2-database.sh
if [ `echo ${MONGO_MAJOR} | cut -d. -f1` == "4" ]
then
 MONGOSH=mongo
else
 MONGOSH=mongosh
fi
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
$MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
db.createCollection("jobControl",
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
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
