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

import datetime
import hazelcast
import logging
import os
import sys
import time

if (len(sys.argv) != 3):
    message = "Expected two args (sys.argv==3), got: " + str(sys.argv)
    sys.exit(message)

print("===============")
args = "\"" + sys.argv[1] + "\" & \"" + sys.argv[2] + "\""
print(sys.argv[0], ":", datetime.datetime.now().strftime("%Y-%m-%d-%H:%M:%S"), " args:", args)

# Null safe
def my_is_true(s):
    if not s:
        return False
    if s.casefold() == "true":
        return True
    return False

cluster_name=  sys.argv[1]
kubernetes = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"
kubernetes_enabled = os.environ.get('MY_KUBERNETES_ENABLED')
print("kubernetes_enabled==", kubernetes_enabled)

cluster_member = kubernetes
if not my_is_true(kubernetes_enabled):
    cluster_member = sys.argv[2]

#
print("cluster_name", cluster_name)
print("cluster_member", cluster_member)

logging.basicConfig(level=logging.INFO)
client = hazelcast.HazelcastClient(
    client_name="@my.docker.image.name@",
    # 5 seconds
    cluster_connect_timeout=5,
    cluster_name=cluster_name,
    cluster_members=[cluster_member],
    labels=["finos"],
    reconnect_mode='OFF',
    statistics_enabled=True
)

map_name = "portfolios"
portfolios = client.get_map(map_name).blocking()

# Test connection
print("---------------")
print(map_name + ".size()==" + str(portfolios.size()))
print("---------------")

## Stay running
#print("Disconnecting")
#client.shutdown()