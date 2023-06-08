# Script is sourced, can't use $0
MYNAME=3-serviceHistory.sh
if [ "${MONGO_MAJOR}" == "4.2" ]
then
 MONGOSH=mongo
else
 MONGOSH=mongosh
fi
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
$MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
print("===================")
db.jobControl.insert( 
   [
     { _id: "01", jobName: "archiver", stateRequired: "SUSPENDED" }
   ] )   
EOF
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
