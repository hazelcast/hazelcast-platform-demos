echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
FILENAME=/tmp/`basename $0`
echo use admin\; > $FILENAME
echo db.grantRolesToUser\(\"@my.other.admin.user@\", \[\"readWriteAnyDatabase\"\], \[\"clusterMonitor\"\]\) >> $FILENAME
echo db.getUser\(\"@my.other.admin.user@\"\) >> $FILENAME
echo use config\; >> $FILENAME
echo db.createUser\( >> $FILENAME
echo \{ user: \'@my.other.admin.user@\', >> $FILENAME  
echo pwd: \'@my.other.admin.password@\', >> $FILENAME  
echo roles: \[ \{ role: \"readWrite\", db: \"config\" \}, \{ role: \"readWrite\", db: \"listCollections\", \{ role: \"readWrite\", db: \"listDatabases\" \} \] >> $FILENAME
echo \} \) >> $FILENAME
echo db.getUser\(\"@my.other.admin.user@\"\) >> $FILENAME
echo show collections >> $FILENAME
echo db.system.sessions.stats\(\) >> $FILENAME
cat $FILENAME
cat $FILENAME | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
if [ "$HOSTNAME" != "mongo" ]
then
 echo "$0: = = = = = = = = = = = = = = = = = = = = = = = = = = = ="
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\'
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\'
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\'
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\'
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\'
 echo "$0: = = = = = = = = = = = = = = = = = = = = = = = = = = = ="
fi