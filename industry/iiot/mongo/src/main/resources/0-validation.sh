# Script is sourced, can't use $0
MYNAME=0-validation.sh
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
if [ "$HOSTNAME" != "mongo" ]
then
 echo ' = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ='
 echo ' = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ='
 echo \$HOSTNAME is \'$HOSTNAME\' not \'mongo\', bad if in K8S, ok on localhost
 echo ' = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ='
 echo ' = = = = = = = = = = = = = = = = = = = = = = = = = = = = = ='
fi
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
