########################################################################
#
# Calculate PI using the Monte Carlo method, variant 2.
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
#  For each point, Python will determine whether the point lies
# within a circle of radius 1.0, and return true or false.
#  So for each input, the output is "true" or "false"
# ----------------------------------------------------------------------
# Note:
#  This Python process does not itself compute Pi, but produces
# values from which Pi can be determined. These values have to be 
# combined with the values from clones of this Python process running
# in parallel to estimate Pi.
# See variant 1 of this (pi1.py) for a different approach.
# ----------------------------------------------------------------------
# Note2:
#  We use "tribool" purely to show how "requirements.txt" are imported by
# Jet when this Python script is deployed to the cluster.
########################################################################
from tribool import Tribool

def handle(points):
    results = []

    for point in points:
      xy = point.split(',')
      x = xy[0]
      y = xy[1]
      x_squared = float(x) * float(x)
      y_squared = float(y) * float(y)
      xy_squared = (x_squared + y_squared)
      if xy_squared <= 1 :
        result = Tribool(True) 
      else :
        result = Tribool(False) 

      results.append(str(result))

    return results