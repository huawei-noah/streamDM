---
layout: page
title: Documentation
image:
  feature: screen_network.png
---
## Big Data Stream Learning 

 Big Data stream learning is more challenging that batch learning, since:

* data is not i.i.d.,  and thus learners need to adapt to changes appropriately
* data examples are processed only once, without storing them in memory
* learning should be very fast and efficient.

## Spark Streaming

Spark Streaming is an extension of the core Spark and Spark Streaming API that
enables stream processing from a variety of sources. Spark is a extensible and
programmable framework for massive distributed processing of datasets, called
Resilient Distributed Datasets (RDD). Spark Streaming receives input data
streams and divides the data into batches, which are then processed by the Spark
engine to generate the results.

Spark Streaming data is organized into a sequence of DStreams, represented
internally as a sequence of RDDs.

## StreamDMM

`TODO: Add words about StreamDM`

### Included Methods

In the first release of StreamDM, we have implemented:

* [SGD Learner](SGD.html) (Logistic Regression,...)
* [Perceptron](SGD.html#perceptron)
* [NB](NB.html)
* [CluStream](CluStream.html)

In the next 4 weeks we plan to release

* Hoeffding Decision Trees
* Random Forests
* Stream KM++

In the next 3 months, we plan to include 

* Frequent Itemset Miner: IncMine
