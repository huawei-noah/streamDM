#!/usr/bin/env python

import socket
import sys
import time
import random
from thread import *

'''
creates a text socket sending dense instance
- clientthread_file takes the contents of a file and sends it
- clientthread generates dense instances randomly, by also adding noise
- by modifying the sltime value (in second) one can control the speed at which instances get sent
'''


HOST = ''
PORT = 9999
features = 3
sltime = 1.0 / float(sys.argv[2] if len(sys.argv) > 2 else 1000)
filein = sys.argv[1] if len(sys.argv) > 1 else 'syn.dat'

def label(feat, func):
  value = 1 if sum([x*y for (x,y) in zip(feat,func[:features-1])])>\
      -func[features] else 0
  return value

def clientthread_file(conn):
  f = open(filein,'r')
  print 'sending instances'
  idx = 0
  while True:
    line = f.readline().strip()
    if not line: pass
    else:
      idx += 1
      if idx%100000==0: print '\tinstace number %d' % idx
      try:
        #print line
        conn.sendall(line+'\n')
        time.sleep(sltime)
      except socket.error as msg:
        print 'Error Code: '+str(msg[0]) + ' Message ' + msg[1]
        break
  print 'finished instances'
  conn.close()
  f.close()
  sys.exit(0)

def clientthread(conn):
  print 'generating function'
  func = [random.uniform(-1,1) for i in range(features+1)]
  fstring = '\t'
  for i in range(features): fstring+='%+.3fx_%d' % (func[i],i)
  fstring += '%+.3f' % func[features]
  print fstring
  print 'generating examples'
  f = open('out_instances.dat','w')
  while True:
    try:
      featgen = [random.uniform(-5,5) for i in range(features)]
      lab = label(featgen,func)
      sendstr = '%d' % lab
      for feat in featgen: sendstr += ',%.3f' % random.gauss(feat,0.5)
      conn.sendall(sendstr+'\n')
      f.write(sendstr+'\n')
      time.sleep(sltime)
    except socket.error as msg:
      print 'Error Code: '+str(msg[0]) + ' Message ' + msg[1]
      break
  conn.close()
  f.close()
  sys.exit(0)

if __name__=='__main__':
  random.seed()

  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  print 'Socket created'

  try:
    s.bind((HOST, PORT))
  except socket.error as msg:
    print 'Bind failed. Error Code: '+str(msg[0]) + ' Message ' + msg[1]
    sys.exit()

  print 'Socket bind complete'

  s.listen(10)
  print 'Socket now listening'

  while 1:
    conn, addr = s.accept()
    print 'Connected with ' + addr[0] + ':' + str(addr[1])
    start_new_thread(clientthread_file, (conn,))
  
  s.close()
