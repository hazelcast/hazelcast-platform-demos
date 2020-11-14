echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
FILENAME=/tmp/`basename $0`
echo use admin\; > $FILENAME
echo db.grantRolesToUser\(\"@my.other.admin.user@\", \[\"userAdminAnyDatabase\", \"dbAdminAnyDatabase\", \"readWriteAnyDatabase\"\]\) >> $FILENAME
echo db.getUser\(\"@my.other.admin.user@\"\) >> $FILENAME
cat $FILENAME
cat $FILENAME | mongo -u @my.other.admin.user@ -p @my.other.admin.password@
echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
