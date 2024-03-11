#
# Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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
import sys

# From py-env paths
import cli_hz

# Don't change, IMAP on cluster to poll
MAP_NAME = "perspective"

console.log("cli.py", "--------------")
console.log("cli.py", "sys.version", sys.version)
console.log("cli.py", datetime.datetime.now().strftime("%Y-%m-%d-%H:%M:%S"))
console.log("cli.py", "--------------")

#https://github.com/pyscript/pyscript/issues/1137
def handler(loop, context):
    console.log("ERROR", context.message)
    raise(context.exception)

pyscript.loop.set_exception_handler(handler)

########################################################################
# Main code, doIt() is run asychronously
########################################################################

logging.basicConfig(level=logging.INFO)

async def doIt():
  console.log("cli.py", "doIt()", "cli_hz.client()", "BEFORE")
  client = await cli_hz.get_client()
  console.log("cli.py", "doIt()", "cli_hz.client()", "AFTER")
  # Change DIV
  Element("loading").clear()
  # FIXME FIXME FIXME
  Element("loaded").write("FIXME FIXME FIXME")
  while True:
    console.log("cli.py", "doIt()", "BEFORE SLEEP LOOP")
    await asyncio.sleep(30)
    console.log("cli.py", "doIt()", "AFTER SLEEP LOOP")

async def main():
   console.log("cly.py", "main()", "START")
   # Never completes, infinite loop
   await doIt()
   console.log("cly.py", "main()", "END")

asyncio.ensure_future(main()) 
