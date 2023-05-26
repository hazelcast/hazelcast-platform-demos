# Script is sourced, can't use $0
MYNAME=1-roles.sh
if [ "${MONGO_MAJOR}" == "4.2" ]
then
 MONGOSH=mongo
else
 MONGOSH=mongosh
fi
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
FILENAME=/tmp/`basename $0`
echo use admin\; > $FILENAME
echo db.grantRolesToUser\(\"@my.other.admin.user@\", \[\"readWriteAnyDatabase\"\, \"clusterMonitor\"\], {} \) >> $FILENAME
echo db.getUser\(\"@my.other.admin.user@\"\) >> $FILENAME
echo use @my.other.admin.database@\; >> $FILENAME
echo db.createUser\( >> $FILENAME
echo \{ user: \'@my.other.admin.user@\', >> $FILENAME  
echo pwd: \'@my.other.admin.password@\', >> $FILENAME  
echo roles: \[ \{ role: \"readWrite\", db: \"admin\" \}, \{ role: \"readWrite\", db: \"@my.other.admin.database@\" \}, \{ role: \"readWrite\", db: \"config\" \}, \{ role: \"readWrite\", db: \"local\" \}, \{ role: \"readWrite\", db: \"listCollections\" \}, \{ role: \"readWrite\", db: \"listDatabases\" \} \] >> $FILENAME
echo \} \) >> $FILENAME
echo use config\; >> $FILENAME
echo db.createUser\( >> $FILENAME
echo \{ user: \'@my.other.admin.user@\', >> $FILENAME  
echo pwd: \'@my.other.admin.password@\', >> $FILENAME  
echo roles: \[ \{ role: \"readWrite\", db: \"admin\" \}, \{ role: \"readWrite\", db: \"@my.other.admin.database@\" \}, \{ role: \"readWrite\", db: \"config\" \}, \{ role: \"readWrite\", db: \"local\" \}, \{ role: \"readWrite\", db: \"listCollections\" \}, \{ role: \"readWrite\", db: \"listDatabases\" \} \] >> $FILENAME
echo \} \) >> $FILENAME
cat $FILENAME
cat $FILENAME | $MONGOSH -u @my.other.admin.user@ -p @my.other.admin.password@
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
