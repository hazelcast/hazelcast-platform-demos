#!/bin/bash

# For pinning, incremental counter. Fails safe, won't assign affinity if request CPU that does not exist
CPU=0

# Job control
set -m

# Logging
echo $# args == $*

# Capture port is optional first argument
PORT=${1}
RANGE=1
if [[ -z "${1}" ]] || ! [[ $1 =~ ^[0-9]+$ ]] ; then
 PORT=50001
else
 PORT=${1}
 shift
 # Range is 2nd argument if port is first
 RANGE=${1}
 shift
fi
echo \$PORT==$PORT
echo \$RANGE==$RANGE
echo $# args == $*

# Start gRPC server in background, first uses CPU 0, second uses CPU 1
while [[ $CPU -lt $RANGE ]] 
do
 ACTUAL_PORT=$(($PORT + $CPU))
 echo /cvarisk/build/cvarisk_server 0.0.0.0:${ACTUAL_PORT} \&
 /cvarisk/build/cvarisk_server 0.0.0.0:${ACTUAL_PORT} 2>&1 &
 PID=$!
 CMD="taskset -p -c $CPU $PID" 
 echo $CMD
 echo XXX turn off: $CMD
 #$CMD
 CPU=$(($CPU + 1))
done

# Start Jet node in background, CPU 0, "-XX:ActiveProcessorCount=2"
echo /usr/bin/java $* -jar application.jar \&
/usr/bin/java $* -jar application.jar 2>&1 &
PID=$!
CMD="taskset -p -c $CPU $PID" 
echo $CMD
echo XXX turn off: $CMD
#$CMD

# Confirm what's running
jobs

# C++ never terminates, foreground Jet
fg
echo fg ends: $?
