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

# Run Node.js
echo REACT_APP_MC_CLUSTER1_NAME=$MC_CLUSTER1_NAME > .env
echo REACT_APP_MC_CLUSTER1_LIST=$MC_CLUSTER1_LIST >> .env
echo REACT_APP_MY_KUBERNETES_ENABLED=$MY_KUBERNETES_ENABLED >> .env
cat .env
echo npm --loglevel=error start
npm --loglevel=error start