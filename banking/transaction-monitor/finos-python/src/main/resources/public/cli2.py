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

from js import console
import hazelcast

########################################################################
# Encapsulates creation of Hazelcast client
########################################################################

async def client():
  console.log("cli2.py", "client()", "BEFORE")
  client = hazelcast.HazelcastClient(
    client_name="@my.docker.image.name@",
    # 5 seconds
    cluster_connect_timeout=5,
    cluster_name=cluster_name,
    cluster_members=[cluster_member],
    # Needed if client is outside Kubernetes and cluster is inside
    smart_routing=False,
    labels=["finos", "build-@maven.build.timestamp@"],
    reconnect_mode='OFF',
    statistics_enabled=True
  )
  console.log("cli2.py", "client()", "AFTER")
  return client
  