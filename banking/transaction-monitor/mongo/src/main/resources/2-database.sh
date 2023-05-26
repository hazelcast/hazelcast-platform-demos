# Script is sourced, can't use $0
MYNAME=2-database.sh
if [ "${MONGO_MAJOR}" == "4.2" ]
then
 MONGOSH=mongo
else
 MONGOSH=mongosh
fi
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
$MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
db.createCollection("xxxneil", {})
show dbs
EOF
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
