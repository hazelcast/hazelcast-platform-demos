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
# Calculate PI using the Monte Carlo method, variant 1.
#
# ----------------------------------------------------------------------
# Algorithm:
#
# See https://en.wikipedia.org/wiki/Monte_Carlo_method#Overview for an
# explanation of how the algorithm works.
#
# ----------------------------------------------------------------------
# Input:
#  Hazelcast Jet will pass the "handle()" function a batch of X & Y
# points for a square of size 1 x 1. 
#  Each is in the format "0.6464563,0.1580038".
# ----------------------------------------------------------------------
# Output:
#  For each point, the determination is made whether the point is
# inside the circle or not, and PI is calculated based on running
# totals in global vairables (in this Python process).
#  For each input point, one output is produced with the current
# approximation for PI, in the format "3.1459265"
# ----------------------------------------------------------------------
# Note:
#  This Python process produces an approximation for PI that needs
# combined with the approximations produced by other Python processes.
# See variant 2 of this (pi2.py) for a different approach.
########################################################################

count_inside = 0
count_all = 0

def handle(points):
    global count_inside
    global count_all

    results = []

    for point in points:
      count_all += 1
      xy = point.split(',')
      x = xy[0]
      y = xy[1]
      x_squared = float(x) * float(x)
      y_squared = float(y) * float(y)
      xy_squared = (x_squared + y_squared)
      if xy_squared <= 1 :
        count_inside += 1

      pi = 4 * count_inside / count_all

      results.append(str(pi))

    return results
