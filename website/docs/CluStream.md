---
layout: page
title: CluStream
image:
  feature: screen_network.png
---

## CluStream Clustering

*Charu C. Aggarwal, Jiawei Han, Jianyong Wang, Philip S. Yu.
A Framework for Clustering Evolving Data Streams. 
VLDB 2003: 81-92*

###General Description

The *CluStream* method is a method of clustering data streams, based on the
concept of *microclusters*. Microclusters are data structures which summarize a
set of instances from the stream, and is composed of a set of statistics which
are easily updated and allow fast analysis.

CluStream has two phases. In the *online* phase, a set of microclusters are kept
in main memory; each instance coming from the input stream can then be either
appended to an existing microcluster or created as a new microcluster. Space for
the new microcluster is created either by deleting a microcluster (by analyzing
its expiration timestamp) or by merging the two closest microclusters. The
*offline* phase will apply a weighted k-means algorithm on the microclusters, to
obtain the final clusters from the stream.

###Implementation

In StreamDM, two classes deal with the implementation of the CluStream
algorithm: `MicroClusters` and `Clustream`.

`MicroClusters` is the main data structure keeping the online microclusters
updated. It is controlled by the following options:

* Horizon parameter (**-h**), which controls the size of horizon window in
  seconds (1000 by default), which controls which microclusters become expired;
* Cluster radius multiplier parameter (**-r**), which controls how large the
  radius of a microcluster is to allow new instances to be appended (2 by
  default); and
* m-value parameter (**-m**) which controls the number of standard deviations
  from the mean timestamp in a microcluster, for expiry analysis (100 by default).

`Clustream` implements the main algorithm, and is controlled by the following
parameters:

* Initial buffer parameter (**-b**), which controls the initial buffer from which
  microclusters are created (1000 by default);
* Number of microclusters parameter (**-m**), which controls how many
  microclusters are kept (100 by default);
* Number of clusters parameter (**-k**), which controls how many clusters are
  output by the offline k-means algorithm (10 by default); and
* k-means iterations parameter (**-i**), which controls for how many iterations the
  k-means algorithm iterates (1000 by default).
