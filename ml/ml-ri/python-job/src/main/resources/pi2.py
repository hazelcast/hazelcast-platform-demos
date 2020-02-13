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
########################################################################

