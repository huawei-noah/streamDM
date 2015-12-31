---
layout: page
title: 'streamDM: Data Mining for Spark Streaming' 
image:
  feature: screen_finance.png
---

streamDM is a new open source software for mining big data streams using [Spark Streaming](https://spark.apache.org/streaming/), developed at [Huawei Noah's Ark
Lab](http://www.noahlab.com.hk/). streamDM is licensed under Apache Software License v2.0.

<p align="center"><img height="100" width="100" src="images/huawei-logo.png" alt="Huawei"></p>

## Big Data Stream Learning 

Big Data stream learning is more challenging than batch or offline learning,
since the data may not keep the same distribution over the lifetime of the
stream. Moreover, each example coming in a stream can only be processed once, or
they need to be summarized with a small memory footprint, and the learning
algorithms must be very efficient. 

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

### <a name="methods"></a>Included Methods

In this first release of StreamDM, we have implemented:

* [SGD Learner](docs/SGD.html) and [Perceptron](docs/SGD.html#perceptron)
* [Naive Bayes](docs/NB.html)
* [CluStream](docs/CluStream.html)
* [Hoeffding Decision Trees](docs/HDT.html)
* [Bagging](docs/Bagging.html)
* [Stream KM++](docs/StreamKM.html)

We have implemented following [data generators](docs/generators.html):

* HyperplaneGenerator
* RandomTreeGenerator
* RandomRBFGenerator
* RandomRBFEventsGenerator

We have also implemented [SampleDataWriter](docs/SampleDataWriter.html), which can call data generators 
to create sample data for simulation or test.

In the next releases we plan to add:

* Classification: Random Forests
* Regression: Hoeffding Regression Tree, Bagging, Random Forests
* Clustering: Clustree, DenStream
* Frequent Itemset Miner: IncMine, IncSecMine

## Going Further

For a quick introduction to running StreamDM, refer to the [Getting
Started](docs/GettingStarted.html) document. The StreamDM [Programming
Guide](docs/Programming.html) presents a detailed view of StreamDM. The full API
documentation can be consulted [here](api/index.html). 

##Mailing lists

###User support and questions mailing list:
<a href="mailto:streamdm-user@googlegroups.com">streamdm-user@googlegroups.com</a>
###Development related discussions:
<a href="mailto:streamdm-dev@googlegroups.com">streamdm-dev@googlegroups.com</a>
