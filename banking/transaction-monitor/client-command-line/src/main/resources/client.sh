#!/bin/bash

#
# Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
########################################################################

cd `dirname $0`

set -m

CLC_DIR=$1
CONFIG_NON_VIRIDIAN=$2
CONFIG_VIRIDIAN=$3

export CLC_CLIENT_NAME='@project.artifactId@'
export CLC_CLIENT_LABELS=`basename $HOME`","`date +"%Y-%m-%dT%H:%M:%S"`

USER=`echo $HOME | cut -c2-`
HOST_IP="${HOST_IP}"
KUBERNETES="${MY_KUBERNETES_ENABLED}"

CONTROL_FILE="/tmp/control.file"
USE_VIRIDIAN_KEY="use.viridian"
VIRIDIAN=`grep "${USE_VIRIDIAN_KEY}" $CONTROL_FILE | tail -1 | cut -d= -f2`
if [ `echo "$VIRIDIAN" | tr '[:upper:]' '[:lower:]'` == "true" ]
then 
 VIRIDIAN=true
else
 VIRIDIAN=false
fi

ONE_MINUTE=60
ONE_DAY=$(($ONE_MINUTE * 60 * 24))

echo "--------------------------------------"
echo MY_KUBERNETES_ENABLED \'${KUBERNETES}\'
echo VIRIDIAN \'${VIRIDIAN}\'
echo "--------------------------------------"

if [ "$VIRIDIAN" == "true" ]
then 
 CONFIG=$CONFIG_VIRIDIAN
else 
 CONFIG=$CONFIG_NON_VIRIDIAN
fi

if [ `echo "$KUBERNETES" | tr A-Z a-z` == "false" ]
then
 TMPFILE=/tmp/`basename $0`.sed.$$
 sed "s/address: \".*/address: ${HOST_IP}/" < $CONFIG > $TMPFILE
 cat $TMPFILE > $CONFIG
fi

export INPUT_FILE=/tmp/`basename $0`.input.$$
touch $INPUT_FILE

cd $CLC_DIR/build

(tail -f $INPUT_FILE | ./clc --config $CONFIG 2>&1 ) &

doIt() {
 echo "$1"
 echo "$1" >> $INPUT_FILE
 sleep 1
}

START_TIME=`date +"%Y-%m-%dT%H:%M:%S%z"`
echo "=================== ${START_TIME} ==================="
echo "Sleeping one minute, so cluster populated with data"
echo sleep ${ONE_MINUTE}
doIt 'SHOW MAPPINGS;'
doIt 'SHOW VIEWS;'
doIt '\map entry-set -n __map-store.mysql_slf4j;'
doIt 'SELECT * FROM "__map-store.mysql_slf4j"'
END_TIME=`date +"%Y-%m-%dT%H:%M:%S%z"`
echo "=================== ${END_TIME} ==================="
echo Sleeping for a day
sleep ${ONE_DAY}
echo Disconnecting
kill %1
