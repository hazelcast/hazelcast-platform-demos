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
echo exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.docker.enabled=$MY_DOCKER_ENABLED \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -jar application.jar
exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.docker.enabled=$MY_DOCKER_ENABLED \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -jar application.jar
