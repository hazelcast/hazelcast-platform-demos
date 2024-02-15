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
import gaussian_predict

# Data, Key and 23 features
item1 = 'data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1'

batch1 = []
batch1.append(item1)

def testit(test, batch):
    lenIn = len(batch)
    print("TEST " + str(test) + ":INPUT SIZE " + str(lenIn))
    for item in batch:
        print("TEST " + str(test) + ":INPUT: " + item)
    results = gaussian_predict.predict(batch)
    lenOut = len(results)
    if lenIn != lenOut:
        print("lenIn=" + str(lenIn))
        print("lenOut=" + str(lenOut))
        raise RuntimeError("Input/Output length mismatch")
    print("TEST " + str(test) + ":OUTPUT SIZE " + str(lenOut))
    for result in results:
        print("TEST " + str(test) + ":GAUSSIAN: returned '" + result + "'")

# TESTS
testit(1, batch1)
