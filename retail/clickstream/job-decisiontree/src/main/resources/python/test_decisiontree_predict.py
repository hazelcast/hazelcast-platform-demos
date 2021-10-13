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
#
# Manual way to test "decisiontree_predict.py"
# Similar to "test_gaussian_predict.py"
# Similar to "test_randomforest_predict.py"
# apart from module import and any algorithm specific tests
# 
########################################################################
import decisiontree_predict

# Data, Key and 23 features
item1 = 'data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1'
item2 = 'data,bbbb,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1'
item3 = 'data,cccc,123,456,1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1'
# All zero
item4 = 'data,dddd,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0'
# All one
item5 = 'data,eeee,123,456,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1'

batch1 = []
batch1.append(item1)
batch1.append(item2)
batch1.append(item3)
batch2 = []
batch2.append(item4)
batch3 = []
batch3.append(item5)

def testit(test, batch):
    lenIn = len(batch)
    print("TEST " + str(test) + ":INPUT SIZE " + str(lenIn))
    for item in batch:
        print("TEST " + str(test) + ":INPUT: " + item)
    results = decisiontree_predict.predict(batch)
    lenOut = len(results)
    if lenIn != lenOut:
        print("lenIn=" + str(lenIn))
        print("lenOut=" + str(lenOut))
        raise RuntimeError("Input/Output length mismatch")
    print("TEST " + str(test) + ":OUTPUT SIZE " + str(lenOut))
    for result in results:
        print("TEST " + str(test) + ":DECISIONTREE: returned '" + result + "'")

# TESTS
testit(1, batch1)
testit(2, batch2)
testit(3, batch3)
