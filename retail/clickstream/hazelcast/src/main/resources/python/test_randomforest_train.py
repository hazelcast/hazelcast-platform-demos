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
########################################################################
#
# Manual way to test randomforest_train.py
# 
########################################################################
import randomforest_train

# Causes exception
item0 = '[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0]'
# 24 features
item1 = '[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0]'
item2 = '[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1]'
item3 = '[1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1]'
# All zero
item4 = '[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]'
# All zero except outcome
item5 = '[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]'
# All one
item6 = '[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]'
# All one except outcome
item7 = '[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0]'


batch0 = []
batch0.append(item0)

batch1 = []
batch1.append(item1)
batch1.append(item2)
batch1.append(item3)

batch2 = []
batch2.append(item1)

batch3 = []

batch4 = []
batch4.append(item4)

batch5 = []
batch5.append(item5)

batch6 = []
batch6.append(item6)

batch7 = []
batch7.append(item7)

def testit(test, batch):
    lenIn = len(batch)
    print("TEST " + str(test) + ":INPUT SIZE " + str(lenIn))
    for item in batch:
        print("TEST " + str(test) + ":INPUT: " + item)
    results = randomforest_train.train_model(batch)
    lenOut = len(results)
    if lenIn != lenOut:
        print("lenIn=" + str(lenIn))
        print("lenOut=" + str(lenOut))
        raise RuntimeError("Input/Output length mismatch")
    print("TEST " + str(test) + ":OUTPUT SIZE " + str(lenOut))
    for result in results:
        l = len(str(result))
        print("TEST " + str(test) + ":RANDOMFOREST: returned " + str(l) + " bytes.")

# TESTS
testit(0, batch0)
testit(1, batch1)
testit(2, batch2)
testit(3, batch3)
testit(4, batch4)
testit(5, batch5)
testit(6, batch6)
testit(7, batch7)
