# Script is sourced, can't use $0
MYNAME=2-database.sh
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
mongosh -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
db.createCollection("@my.mongo.collection1.name@", {})
show dbs
EOF
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
