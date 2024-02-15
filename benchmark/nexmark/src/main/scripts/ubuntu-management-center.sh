#!/bin/bash

HEAP_SIZE=24g

MODULE=management-center
JAVA_ARGS="${JAVA_ARGS} -Xms${HEAP_SIZE} -Xmx${HEAP_SIZE}"
JAR_FILE=`ls ./hazelcast-management-center-*.jar`

JAVA_OPTS="\
 --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED \
 --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED \
 --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

CMD="java -server $JAVA_ARGS $JAVA_OPTS -cp $JAR_FILE org.springframework.boot.loader.JarLauncher"
echo $CMD

nohup $CMD > log.$MODULE 2>&1 &
RC=$?
echo RC=${RC}
exit ${RC}
