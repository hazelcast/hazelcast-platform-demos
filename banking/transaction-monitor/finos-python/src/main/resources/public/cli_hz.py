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

from datetime import datetime
from js import console
import hazelcast
from hazelcast.discovery import HazelcastCloudDiscovery
import os

# From py-env paths
import custom_entrypoint

########################################################################
# Cluster connection setup
########################################################################

cluster_name = "@my.cluster1.name@"
instance_name = "@project.artifactId@" 
service_dns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"

viridianId = "@my.viridian.cluster1.id@"
viridianDiscoveryToken = "@my.viridian.cluster1.discovery.token@"
viridianKeyPassword = "@my.viridian.cluster1.key.password@"

controlFile = "/tmp/control.file"
useViridianKey = "use.viridian"
viridianCaFile = "/tmp/ca.pem"
viridianCertFile = "/tmp/cert.pem"
viridianKeyFile = "/tmp/key.pem"

kubernetes = custom_entrypoint.MY_KUBERNETES_ENABLED
host_ip = custom_entrypoint.HOST_IP
user = custom_entrypoint.HOME[1:]

member = service_dns
if kubernetes.lower() == 'false':
    member = host_ip
current_date = datetime.now()
launch_time = current_date.strftime('%Y-%m-%dT%H:%M:%S')

def isViridian():
    if str(custom_entrypoint.USE_VIRIDIAN).lower() == "true":
        return True
    return False

viridian = isViridian()

console.log("cli_hz.py", "--------------")
console.log("cli_hz.py", "viridian", viridian)
if viridian:
  console.log("cli_hz.py", "viridianId", viridianId)
else:
  console.log("cli_hz.py", "member", member)
console.log("cli_hz.py", "launch_time", launch_time)
console.log("cli_hz.py", "user", user)
console.log("cli_hz.py", "--------------")

########################################################################
# Encapsulates creation of Hazelcast client
########################################################################

async def get_client():
  console.log("cli_hz.py", "get_client()", "BEFORE")
  try:
    if viridian:
      console.log("cli_hz.py", "get_client()", "Using Viridian", viridianId)
      #FIXME Should become default
      HazelcastCloudDiscovery._CLOUD_URL_BASE = "api.viridian.hazelcast.com"
      client = hazelcast.HazelcastClient(
        client_name=instance_name,
        cluster_name=viridianId,
        cloud_discovery_token=viridianDiscoveryToken,
        labels=[user, launch_time],
        statistics_enabled=True,
        ssl_enabled=True,
        ssl_cafile=os.path.abspath(viridianCaFile),
        ssl_certfile=os.path.abspath(viridianCertFile),
        ssl_keyfile=os.path.abspath(viridianKeyFile),
        ssl_password=viridianKeyPassword,
      )
    else:
      console.log("cli_hz.py", "get_client()", "Non-Viridian", cluster_name, member)
      client = hazelcast.HazelcastClient(
        client_name=instance_name,
        cluster_name=cluster_name,
        cluster_members=[member],
        labels=[user, launch_time],
        statistics_enabled=True
      )
  except Exception as e:
    console.log("cli_hz.py", "get_client()", "EXCEPTION", str(e))
  console.log("cli_hz.py", "get_client()", "AFTER")
  return client
