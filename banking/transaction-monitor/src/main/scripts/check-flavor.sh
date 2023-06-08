ARG1=`echo $1 | awk '{print tolower($0)}'`
if [ "${ARG1}" == "ecommerce" ]
then
 FLAVOR=ecommerce
fi
if [ "${ARG1}" == "payments" ]
then
 FLAVOR=payments
fi
if [ "${ARG1}" == "trade" ]
then
 FLAVOR=trade
fi

if [ "${FLAVOR}" == "" ] && [ "$ARG1" != "" ]
then
 echo $0: usage: `basename $0` '<flavor>'
 exit 1
else
 # CWD is ${MODULE}
 POM_FLAVOR=`grep '<my.transaction-monitor.flavor>' ../../../pom.xml | tail -1 | cut -d'>' -f2 | cut -d'<' -f1`
 FLAVOR=$POM_FLAVOR
fi
