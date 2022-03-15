#!/bin/bash

if [ $# -ne 2 ]
then
 echo $0: usage: $0 '<ip-of-management-center>' '<user-auth-token>'
 echo $0: example: $0 33.44.55.66 abcdefghijklmnopqrstuvwxyz1234567890abcdefgh
 echo $0: port is set at 8080
 echo $0: mc-conf gives user auth token. See retail/clickstream/management-center/Dockerfile
 exit 1
fi

IP=$1
USER_AUTH_TOKEN=$2
RULE_NAME=$USER
TMPFILE=/tmp/`basename ${0}`.$$

myRestCall() {
 echo " "
 # Could use a file if many more headers
 HEADER1="Authorization: Bearer $USER_AUTH_TOKEN"
 HEADER2="Accept: application/json"
 HEADER3="Content-type: application/json"
 URL=http://${IP}:8080/rest/$1
 if [ $# -eq 2 ]
 then
  # POST is inferred with -d
  echo curl -H \"$HEADER1\" -H \"$HEADER2\" -H \"$HEADER3\" $URL -d \'$2\' >> $TMPFILE 2> /dev/null
  curl -v -H "$HEADER1" -H "$HEADER2" -H "$HEADER3" $URL -d "$2" >> $TMPFILE 2> /dev/null
 else
  echo curl -H \"$HEADER1\" -H \"$HEADER2\" $URL >> $TMPFILE 2> /dev/null
  curl -H "$HEADER1" -H "$HEADER2" $URL >> $TMPFILE 2> /dev/null
 fi
 # Failure is returned as HTTP 400 but Curl returns 0 exit code for this. Non-zero is a bad argument, etc
 RC=$?
 cat $TMPFILE
 echo " "
 /bin/rm $TMPFILE > /dev/null 2>&1
 echo RC=${RC}
 echo " "
}

echo Validate REST available, at given IP \& token:
myRestCall license

echo Clients before:
myRestCall clusters/blue/clients

echo Uploading rule: rule name: \"${RULE_NAME}\"
myRestCall clusters/blue/clientfiltering/lists '{"name":"'${RULE_NAME}'","status":"ACTIVE","type":"DENYLIST","entries":[{"type":"LABEL","value":"webapp"}]}'

echo Listing rules:
myRestCall clusters/blue/clientfiltering/lists

echo Deploying rules:
myRestCall clusters/blue/clientfiltering/deploy '{"status":"ENABLED","type":"DENYLIST"}'

echo Clients after:
myRestCall clusters/blue/clients
