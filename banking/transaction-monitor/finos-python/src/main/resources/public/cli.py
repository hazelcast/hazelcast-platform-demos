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

import asyncio
from js import console
import datetime
import logging

# From py-env paths
import custom_entrypoint
import cli2

########################################################################
# Main code, doIt() is run asychronously
########################################################################

MAP_NAME = "perspective"

# Kubernetes assumed if blank or explicitly configured
def kubernetes_enabled():
    if not custom_entrypoint.MY_KUBERNETES_ENABLED:
        return True
    if custom_entrypoint.MY_KUBERNETES_ENABLED.casefold() != "false":
        return True
    return False

console.log("cli.py", "--------------")
console.log("cli.py", datetime.datetime.now().strftime("%Y-%m-%d-%H:%M:%S"))
console.log("cli.py", "MC_CLUSTER1_NAME", custom_entrypoint.MC_CLUSTER1_NAME)
console.log("cli.py", "MC_CLUSTER1_LIST", custom_entrypoint.MC_CLUSTER1_LIST)
kubernetes = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"

cluster_name= custom_entrypoint.MC_CLUSTER1_NAME
cluster_member = kubernetes
if not kubernetes_enabled():
    cluster_member = custom_entrypoint.MC_CLUSTER1_LIST

console.log("cli.py", "Will use '" + cluster_member + "' for connection.")
console.log("cli.py", "--------------")

logging.basicConfig(level=logging.INFO)

async def doit():
  console.log("cli.py", "doIt()", "BEFORE ---------------", cluster_name)
  client = cli2.client()
  console.log("cli.py", "doIt()", "AFTER  ---------------", cluster_name)
  # Change DIV
  Element("loading").clear()
  # FIXME FIXME FIXME
  Element("loaded").write("FIXME FIXME FIXME")
  while True:
    console.log("cli.py", "doIt()", "BEFORE SLEEP LOOP")
    await asyncio.sleep(30)
    console.log("cli.py", "doIt()", "AFTER SLEEP LOOP")

pyscript_loader.close()
# Never completes, infinite loop
pyscript.run_until_complete(doit())
