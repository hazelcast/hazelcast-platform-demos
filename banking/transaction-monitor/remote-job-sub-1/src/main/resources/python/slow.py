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
########################################################################
# Simplist assessment of whether current trading in a stock is due to
# institutional investor making few large transactions or multiple individual
# investors making small transactions with a similar cumulative effect.
# ----------------------------------------------------------------------
# Input:
# A series of transactions in JSON format:
# {"id": "c6dcc88c-5f1c-4c0d-ae7f-6518e5bb187a","timestamp": 1617797114225,"symbol": "GEVO","price": 2499,"quantity": 5913}
# ----------------------------------------------------------------------
# Output:
# Assessments of stock trading, in CSV format:
# GEVO,WHALE
# ----------------------------------------------------------------------
# Note:
# (1) The same item may feature more than once in the input item list,
# but this isn't considered. Each item is assessed in isolation,
# missing the chance for added value.
# (2) The code has a "sleep()" to simulate complex processing that
# takes a while to run.
########################################################################

import json
import time 

flavor = "@my.transaction-monitor.flavor@"

def processFn(items):
    results = []

    keyField = ""
    thresholdField = ""
    threshold = 0
    thresholdAbove = ""
    thresholdBelow = ""
    # Not Python 3.10 necessarily 
    if flavor == "ecommerce":
      keyField = "itemCode"
      thresholdField = "quantity"
      threshold = 1
      thresholdAbove = "Hot"
      thresholdBelow = "Cold"
    if flavor == "payments":
      thresholdField = "amtFloor"
      keyField = "bicCreditor"
      threshold = 500000
      thresholdAbove = "Whale"
      thresholdBelow = "Minnow"
    if flavor == "trade":
      thresholdField = "quantity"
      keyField = "symbol"
      threshold = 5000
      thresholdAbove = "Whale"
      thresholdBelow = "Minnow"
    if keyField == "":
      return results

    for item in items:
      transaction = json.loads(item)
      key = transaction[keyField]

      if transaction[thresholdField] > threshold:
        assessment = thresholdAbove
      else:
        assessment = thresholdBelow
      
      time.sleep(0.05)

      results.append("".join((key, ",", assessment, ",", flavor)))
    return results    
