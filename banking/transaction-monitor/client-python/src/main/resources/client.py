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

from datetime import datetime
import hazelcast
from hazelcast.sql import SqlColumnType
import logging
import os
import time

cluster_name = "@my.cluster1.name@"
instance_name = "@project.artifactId@" 
service_dns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"

generic_record_map = "__map-store.mysql_slf4j"
logging.basicConfig(level=logging.INFO)

kubernetes = os.environ.get('MY_KUBERNETES_ENABLED')
host_ip = os.environ.get('HOST_IP')
user = os.environ.get('HOME')[1:]

member = service_dns
if kubernetes.lower() == 'false':
    member = host_ip
current_date = datetime.now()

def run_sql_query(client: hazelcast.HazelcastClient, query: str):
    print("--------------------------------------", flush=True)
    print(query)
    try:
        count = 0
        result = client.sql.execute(query).result()
        for row in result:
            i = 0
            for row_metadata in row.metadata.columns:
                if i > 0:
                    print(" ", end = '')
                sqltype = row_metadata.type
                column_metadata = row.metadata.get_column(i)
                column_name = column_metadata.name
                column = row.get_object(column_name)
                if (sqltype == SqlColumnType.VARCHAR):
                    print(column, end = '')
                else:    
                    if (sqltype == SqlColumnType.TIMESTAMP):
                        timestamp_iso8601 = str(column).replace(" ", "T")
                        print(timestamp_iso8601, end = '')
                    else:
                        print("Unhandled Type for Column '" + str(sqltype) + "'", end = '')
                i = i + 1
            print("", flush=True)
        print("[" + str(count) + " rows]", flush=True)
    except Exception as e:
        print(str(e), flush=True)

def list_distributed_objects(client: hazelcast.HazelcastClient):
    print("--------------------------------------", flush=True)
    print("Distributed Objects (excluding system objects)", flush=True)
    count = 0
    for distributed_object in client.get_distributed_objects():
        name = distributed_object.name
        if not (name.startswith("__")):
            service_name = distributed_object.service_name
            print(service_name + " => '" + name + "'", flush=True)
            count = count + 1
    print("[" + str(count) + " rows]", flush=True)

def get_generic_record(client: hazelcast.HazelcastClient):
    print("--------------------------------------", flush=True)
    print("GenericRecord, map \"" + generic_record_map + "\"", flush=True)
    map = client.get_map(generic_record_map)
    count = 0
    for key in map.key_set().result():
        value = map.get(key)
        print(str(key) + "," + str(value), flush=True)
        count = count + 1
    print("[" + str(count) + " rows]", flush=True)

print("--------------------------------------", flush=True)
print("MY_KUBERNETES_ENABLED '", kubernetes, "'", flush=True)
current_date = datetime.now()
launch_time = current_date.strftime('%Y-%m-%dT%H:%M:%S')
client = hazelcast.HazelcastClient(
    client_name=instance_name,
    cluster_name=cluster_name,
    cluster_members=[member],
    labels=[user, launch_time],
    statistics_enabled=True
)
print("--------------------------------------", flush=True) 

current_date = datetime.now()
start_time = current_date.strftime('%Y-%m-%dT%H:%M:%S')
print("===================", start_time, "===================", flush=True)

print("Sleeping one minute, so cluster populated with data", flush=True)
time.sleep(60)

run_sql_query(client, "SHOW MAPPINGS")
run_sql_query(client, "SHOW VIEWS")

list_distributed_objects(client)

get_generic_record(client)

run_sql_query(client, "SELECT * FROM \"" + generic_record_map + "\"")

current_date = datetime.now()
end_time = current_date.strftime('%Y-%m-%dT%H:%M:%S')
print("===================", end_time, "===================", flush=True)
print("Sleeping for a day", flush=True)
time.sleep(24 * 60 * 60)
print("Disconnecing", flush=True)
client.shutdown()