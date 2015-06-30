---
layout: page
title: StreamKM++
image:
  feature: screen_network.png
---

## StreamKM++ Clustering

*Ackermann, Lammersen, Märtens, Raupach, Sohler, Swierkot: 
"StreamKM++: A Clustering Algorithm for Data Streams". 
In Proceedings of the 12th Workshop on Algorithm Engineering and Experiments (ALENEX 2010)*

###General Description

*StreamKM++* computes a small weighted sample of the data stream, which is called 
the coreset of the data stream. A new data structure called coreset tree was 
developed in order to significantly speed up the time necessary for sampling 
non-uniformly during the coreset construction. After the coreset was extracted 
from the data stream, a weighted k-means algorithm could be applied on the coreset 
to get the final clusters for the orginal stream data.

###Implementation

In StremDM, three classes deal with the implementation of the StreamKM++
algorithm: `StreamKM`, `BucketManager`, and 'TreeCoreset'.

`BucketManger` and 'TreeCoreset' are the data structures to deal with the coreset 
tree construction process, it is not necessary for the user to set any parameters
for the class 'TreeCoreset', its behavior is determined by the parameters of the
class 'BucketManager':

* Stream data window size parameter (**-n**), which controls the stream data size 
(100000 by default), actually its value is inherited from the parameters of 
class 'StreamKM';
* coreset size parameter (**-maxsize**) which controls the number of examples in 
the coreset for the stream data (10000 by default).

`StreamKM` implements the main algorithm, and is controlled by the following
parameters:

* Number of clusters parameter (**-k**), which controls how many clusters are
  output by the offline k-means algorithm (10 by default); 
* k-means iterations parameter (**-i**), which controls for how many iterations the
  k-means algorithm iterates (1000 by default);
* Coreset size parameter(**-s**), which controls the number of examples in the coreset
  for StreamKM++ algorithm;
* Windows size parameter(**-w**), which controls how many examples in the stream data
  are used for coreset extraction for StreamKM++ algorithm .
