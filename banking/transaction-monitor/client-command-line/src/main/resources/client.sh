#!/bin/bash

#
# Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# # http://www.apache.org/licenses/LICENSE-2.0
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
CONFIG_NON_HZ_CLOUD=$2
CONFIG_HZ_CLOUD=$3

export CLC_CLIENT_NAME='@project.artifactId@'
export CLC_CLIENT_LABELS=`basename $HOME`","`date +"%Y-%m-%dT%H:%M:%S"`

USER=`echo $HOME | cut -c2-`
HOST_IP="${HOST_IP}"
KUBERNETES="${MY_KUBERNETES_ENABLED}"

CONTROL_FILE="/tmp/control.file"
USE_HZ_CLOUD_KEY="use.hz.cloud"
HZ_CLOUD=`grep "${USE_HZ_CLOUD_KEY}" $CONTROL_FILE | tail -1 | cut -d= -f2`
if [ `echo "$HZ_CLOUD" | tr '[:upper:]' '[:lower:]'` == "true" ]
then 
 HZ_CLOUD=true
else
 HZ_CLOUD=false
fi

ONE_MINUTE=60
ONE_DAY=$(($ONE_MINUTE * 60 * 24))

echo "--------------------------------------"
echo MY_KUBERNETES_ENABLED \'${KUBERNETES}\'
echo HZ_CLOUD \'${HZ_CLOUD}\'
echo "--------------------------------------"

if [ "$HZ_CLOUD" == "true" ]
then 
 CONFIG=$CONFIG_HZ_CLOUD
else 
 CONFIG=$CONFIG_NON_HZ_CLOUD
fi

if [ `echo "$KUBERNETES" | tr A-Z a-z` == "false" ]
then
 TMPFILE=/tmp/`basename $0`.sed.$$
 sed "s/address: \".*/address: ${HOST_IP}/" < $CONFIG > $TMPFILE
 cat $TMPFILE > $CONFIG
fi

export INPUT_FILE=/tmp/`basename $0`.input.$$
touch $INPUT_FILE

cd $CLC_DIR

(tail -f $INPUT_FILE | /root/.hazelcast/bin/clc --config $CONFIG 2>&1 ) &

doIt() {
 echo "$1"
 echo "$1" >> $INPUT_FILE
 sleep 1
 echo "."
}

START_TIME=`date +"%Y-%m-%dT%H:%M:%S%z"`
echo "=================== ${START_TIME} ==================="
echo "Sleeping one minute, so cluster populated with data"
echo sleep ${ONE_MINUTE}
doIt 'SHOW MAPPINGS;'
sleep 5
doIt 'SHOW VIEWS;'
sleep 5
doIt '\map entry-set -n mysql_slf4j'
sleep 5
doIt 'SELECT * FROM "__map-store.mysql_slf4j";'
END_TIME=`date +"%Y-%m-%dT%H:%M:%S%z"`
echo "=================== ${END_TIME} ==================="
echo Sleeping for a day
sleep ${ONE_DAY}
echo Disconnecting
kill %1
