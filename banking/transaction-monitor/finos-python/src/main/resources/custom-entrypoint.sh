#!/bin/bash

# Apply local tweaks if file mounted in container (only Kubernetes)
echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
CUSTOMFILE=/customize/my-env.sh
if [ -f $CUSTOMFILE ]
then
 echo "$0: apply $CUSTOMFILE" 
 source $CUSTOMFILE
else
 echo "$0: no $CUSTOMFILE to apply" 
fi
echo "$0: - - - - - - - - - - - - - - - - - - - - - - - - - - - -"

# Configure address for Kubernetes or Docker
MC_EMPTY=''
MC_CLUSTER1_LIST=`echo ${MC_CLUSTER1_ADDRESSLIST_OVERRIDE:-${MC_EMPTY}} $MC_CLUSTER1_ADDRESSLIST |awk '{print $1}'`

# index.html imports custom-entrypoint.py
DIR=/finos
FILE=$DIR/custom_entrypoint.py
echo MC_CLUSTER1_LIST = \'$MC_CLUSTER1_LIST\' >> $FILE
echo MC_CLUSTER1_NAME = \'$MC_CLUSTER1_NAME\' >> $FILE
echo MY_KUBERNETES_ENABLED = \'$MY_KUBERNETES_ENABLED\' >> $FILE
cat $FILE

python -m http.server --directory $DIR 8080
