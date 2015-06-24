---
layout: page
title: Documentation
image:
  feature: screen_network.png
---
## Big Data Stream Learning 

Big Data stream learning is more challenging than batch or offline learning,
since the data may not keep the same distribution over the lifetime of the
stream. Moreover, each example coming in a stream can only be processed once, or
they need to be summarized with a small memory footprint, and the learning
algorithms must be very efficient. 

## StreamDM

StreamDM is a new open-source library for data analytics on stream, built on top
of Spark Streaming. It is initiated and developed at [Huawei Noah's Ark
Lab](http://www.noahlab.com.hk/).

The tools and algorithms in StreamDM are specifically designed for the data
stream setting, and is the first library to include advanced stream mining
algorithms for Spark Streaming. Its objective is to be the gathering point of
practical implementations and deployments for large-scale data stream analytics.

### Spark Streaming

[Spark Streaming](https://spark.apache.org/streaming/) is an extension of the
core [Spark](https://spark.apache.org)  API that enables stream processing from
a variety of sources. Spark is a extensible and programmable framework for
massive distributed processing of datasets, called Resilient Distributed
Datasets (RDD). Spark Streaming receives input data streams and divides the data
into batches, which are then processed by the Spark engine to generate the
results.

Spark Streaming data is organized into a sequence of DStreams, represented
internally as a sequence of RDDs.

### Included Methods

In the first release of StreamDM, we have implemented:

* [SGD Learner](SGD.html) and [Perceptron](SGD.html#perceptron)
* [Naive Bayes](NB.html)
* [CluStream](CluStream.html)
* [Hoeffding Decision Trees](HDT.html)
* Stream KM++

In the next releases we plan to add: 

* Random Forests
* Frequent Itemset Miner: IncMine

## Going Further

For a quick introduction to running StreamDM, refer to the [Getting
Started](GettingStarted.html) document. The StreamDM [Programming
Guide](Programming.html) presents a detailed view of StreamDM. The full API
documentation can be consulted here. 
