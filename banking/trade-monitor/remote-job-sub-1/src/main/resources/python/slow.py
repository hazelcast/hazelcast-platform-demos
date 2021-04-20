#
# Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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
# Simplist assessment of whether current trading in a stock is due to
# institutional investor making few large trades or multiple individual
# investors making small trades with a similar cumulative effect.
# ----------------------------------------------------------------------
# Input:
# A series of trades in JSON format:
# {"id": "c6dcc88c-5f1c-4c0d-ae7f-6518e5bb187a","timestamp": 1617797114225,"symbol": "GEVO","price": 2499,"quantity": 5913}
# ----------------------------------------------------------------------
# Output:
# Assessments of stock trading, in CSV format:
# GEVO,WHALE
# ----------------------------------------------------------------------
# Note:
# (1) The same stock may feature more than once in the input item list,
# but this isn't considered. Each item is assessed in isolation,
# missing the chance for added value.
# (2) The code has a "sleep()" to simulate complex processing that
# takes a while to run.
########################################################################

import json
import time 

def processFn(items):
    results = []

    for item in items:
      trade = json.loads(item)
      symbol = trade["symbol"]

      if trade["quantity"] > 5000:
        assessment = "WHALE"
      else:
        assessment = "MINNOW"
      
      time.sleep(0.05)

      results.append("".join((symbol, ",", assessment)))
    return results    
