---
layout: page
title: CluStream
image:
  feature: screen_network.png
---

## CluStream Clustering

*CluStream* is the first clustering method that presents and uses the notion of micro-clusters as the output result of the online process. Micro-clusters are the statistics kept from the data in a fast way in the online phase. However, an offline phase is needed to improve the quality of the clustering.

In CluStream, each micro-cluster maintains online a Clustering Feature. Clustering Features \\\(CF = (N,LS,SS, LT, ST)\\) are extensions of BIRCH CF with two additional temporal features: 

* N: number of data points
* LS: linear sum of the N data points
* SS: square sum of the N data points
* LT: n-dimensional linear sum of the time stamps
* ST: n-dimensional square sum of the time stamps
 
The CluStream method has two phases, an on-line and off-line. The on-line phase keeps a maximum number of microclusters in memory, and every time a new instance arrives, it updates the statistics related to this microcluster. First, it looks if the new point can be absorbed by one of the current microclusters. If not, it tries to start a new micro-cluster of its own. If there is no space for a new micro-cluster, it reduces space using one of these two strategies: deleting the oldest micro-cluster, or merging two of the oldest micro-clusters.

The off-line phase, simply periodically applies k-means using microclusters as points or instances.

*Charu C. Aggarwal, Jiawei Han, Jianyong Wang, Philip S. Yu.
A Framework for Clustering Evolving Data Streams. 
VLDB 2003: 81-92*



