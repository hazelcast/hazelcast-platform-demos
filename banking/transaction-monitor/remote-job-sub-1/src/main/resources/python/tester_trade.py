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
# Tests the main Python module
########################################################################

import slow
import time

transaction1 = '{"id": "c6dcc88c-5f1c-4c0d-ae7f-6518e5bb187a","timestamp": 1617797114225,"symbol": "GEVO","price": 2499,"quantity": 5913}'
transaction2 = '{"id": "6edfd130-d088-491b-a36a-934c029729a7","timestamp": 1617874250739,"symbol": "PLAB","price": 2501,"quantity": 4408}'
transaction3 = '{"id": "0f1a39a7-40c3-4f5a-bf05-c57229e9aebe","timestamp": 1617878692183,"symbol": "VIRC","price": 2499,"quantity": 5379}'

print("----------------------------------")
print("Run from target/classes/python to use slow.py with Maven substitutions.")
print("Run the tester appropriate to my.transaction-monitor.flavor=@my.transaction-monitor.flavor@")
print("----------------------------------")

batch = []
batch.append(transaction1)
batch.append(transaction2)
batch.append(transaction3)

for item in batch:
    print("INPUT: " + item)

before = time.perf_counter()
results = slow.processFn(batch)
elapsed = time.perf_counter() - before

for result in results:
    print("OUTPUT: " + result)

print("ELAPSED: " + str(elapsed) + " seconds")

if len(results) != len(batch):
    print("FAIL")
    print("Batch length", len(batch))
    print("Results lenght", len(results))
    print("FAIL")
