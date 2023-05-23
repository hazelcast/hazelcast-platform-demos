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

# Run Java
exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.cpp.service=$MY_CPP_SERVICE \
 -Dmy.docker.enabled=true \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -Dmy.site=$MY_SITE \
 -Dmy.threshold=$MY_THRESHOLD \
 -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener \
 -jar application.jar $0 $1
