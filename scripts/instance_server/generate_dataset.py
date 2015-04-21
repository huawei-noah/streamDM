#!/usr/bin/env python

import socket
import sys
import time
import random
from thread import *

'''
creates a file generating dense instances randomly, by also adding noise
'''


features = 3
numInstances = 1000000
fileoutput = 'syn.dat'

def label(feat, func):
  value = 1 if sum([x*y for (x,y) in zip(feat,func[:features-1])])>\
      -func[features] else 0
  return value

def generate():
  print 'generating function'
  func = [random.uniform(-1,1) for i in range(features+1)]
  fstring = '\t'
  for i in range(features): fstring+='%+.3fx_%d' % (func[i],i)
  fstring += '%+.3f' % func[features]
  print fstring
  print 'generating examples'
  f = open(fileoutput,'w')
  for x in range (0, numInstances):
    featgen = [random.uniform(-5,5) for i in range(features)]
    lab = label(featgen,func)
    sendstr = '%d ' % lab
    for feat in featgen: sendstr += '%.3f,' % random.gauss(feat,0.5)
    f.write(sendstr[:-1]+'\n')
  f.close()

if __name__=='__main__':
  random.seed()
  generate() 

  print str(numInstances)+' instances succesfully generated in file '+fileoutput+'!'

