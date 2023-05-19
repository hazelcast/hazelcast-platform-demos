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
#
# Decision Tree prediction mechanism.
#
# ----------------------------------------------------------------------
# Input:
#  CSV data with a control command.
#  If CSV begins "data," the next fields are the clickstream key, two timestamps, 
# then 23 true/false values (0==false, 1==true) for clickstream actions.
#  Eg. "data,neil,123,456,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1"
#  If the CSV begins with anything else, it is ignored
# ----------------------------------------------------------------------
# Output:
#  CSV data with a control command
#  For input command "data," output is "data," + clickstream key and timestamps,
# the model version, and finally buy or not-buy prediction (0==not-buy, 1==buy). 
# So for above input the output "neil,123,456,somethig,1" for a buy prediction
# ------------s----------------------------------------------------------
########################################################################
import random

# Replaced by Maven
version = "@maven.build.timestamp@"

def predict(input_list):
    global version
    result = []

    for entry in input_list:
        values = entry.replace(", ", ",").split(",")
        if values[0] == "data":
            # append values to features
            key = values[1]
            publish = values[2]
            ingest = values[3]
            diagnostic = ""
            values = [int(it) for it in values[4:]]
            #FIXME Guess for now
            prediction = [random.randint(0,1)]
            result.append(str(key) + "," + str(publish) + "," + str(ingest) + "," + version + "," + str(prediction[0]) + "," + diagnostic)
        else:
            raise NotImplementedError('Unexpected control', values[0])

    return result
