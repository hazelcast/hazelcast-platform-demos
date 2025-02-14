#
# Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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
import random

def assess(items):

    results = []

    # Simple ML, each dropped call doubles the churn likelyhood.
    # Better algorithms exist, substitute your own.
    for item in items:
      csv = item.split(",")

      # Call dropped
      dropped_str = csv[5]
      if dropped_str.lower() == "true":
        dropped = True
      else:
        dropped = False

      # Current sentiment
      current_str = csv[21]
      if current_str == "":
        current_pct = float(0.0)
      else:
        current_pct = float(current_str)

      # Previous sentiment
      previous_str = csv[22]
      if previous_str == "":
        previous_pct = float(0.0)
      else:
        previous_pct = float(previous_str)

      #####################################
      # Business logic
      # --------------
      # Step up by more annoyance each time
      # a call is dropped. Don't yet reduce
      # level if lots of successful calls.
      # Add some randomness so all data
      # not the exact same.
      #####################################
      if dropped:
        old_annoyance = current_pct - previous_pct
        new_annoyance = float(3.1) + old_annoyance + current_pct + random.random()
      else:
        new_annoyance = current_pct
      #####################################
      if new_annoyance > float(100.0):
        new_annoyance = float(100.0)
      #####################################

      # Append new_annoyance level to original input, plus the input current annoyance becomes output previous annoyance
      results.append(item +  "," + str(new_annoyance) + "," + str(current_pct))

    return results
