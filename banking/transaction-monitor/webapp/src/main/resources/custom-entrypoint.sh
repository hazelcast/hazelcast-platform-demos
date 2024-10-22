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
echo java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS \
 -Dmy.cassandra.address=$MY_CASSANDRA_ADDRESS \
 -Dmy.docker.enabled=$MY_DOCKER_ENABLED \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -Dmy.maria.address=$MY_MARIA_ADDRESS \
 -Dmy.mongo.address=$MY_MONGO_ADDRESS \
 -Dmy.mysql.address=$MY_MYSQL_ADDRESS \
 -Dmy.postgres.address=$MY_POSTGRES_ADDRESS \
 -Dmy.pulsar.address=$MY_PULSAR_ADDRESS \
 -cp ./application.jar:./namespace1.jar:./namespace2.jar:./namespace3.jar \
 $MAIN_CLASS
exec java $JAVA_ARGS $JAVA_OPTS \
 -Dmy.bootstrap.servers=$MY_BOOTSTRAP_SERVERS \
 -Dmy.cassandra.address=$MY_CASSANDRA_ADDRESS \
 -Dmy.docker.enabled=$MY_DOCKER_ENABLED \
 -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED \
 -Dmy.maria.address=$MY_MARIA_ADDRESS \
 -Dmy.mongo.address=$MY_MONGO_ADDRESS \
 -Dmy.mysql.address=$MY_MYSQL_ADDRESS \
 -Dmy.postgres.address=$MY_POSTGRES_ADDRESS \
 -Dmy.pulsar.address=$MY_PULSAR_ADDRESS \
 -cp ./application.jar:./namespace1.jar:./namespace2.jar:./namespace3.jar \
 $MAIN_CLASS
