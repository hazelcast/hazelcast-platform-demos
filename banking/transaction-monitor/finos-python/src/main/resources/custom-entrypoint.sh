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

USE_VIRIDIAN=`grep use.viridian /tmp/control.file | cut -d= -f2`

# index.html imports custom-entrypoint.py
DIR=/finos
FILE=$DIR/custom_entrypoint.py
echo MY_KUBERNETES_ENABLED = \'$MY_KUBERNETES_ENABLED\' >> $FILE
echo HOST_IP = \'$HOST_IP\' >> $FILE
echo HOME = \'$HOME\' >> $FILE
echo USE_VIRIDIAN = \'$USE_VIRIDIAN\' >> $FILE
cat $FILE

python -m http.server --directory $DIR 8080
