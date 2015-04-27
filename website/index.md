---
layout: page
title: 'streamDM: Data Mining for Spark Streaming' 
image:
  feature: screen_finance.png
---

streamDM is an open source software for mining Big Data streams using Spark Streaming. streamDM is licensed under an Apache Software License v2.0.

<p align="center"><img height="100" width="100" src="images/huawei-logo.png" alt="Huawei"></p>

# Spark Streaming

Spark Streaming is an extension of the core Spark API that enables scalable, high-throughput, fault-tolerant stream processing of data streams.
StreamDM is designed to be easily extended and used, and is the first library to contain advanced stream mining algorithms for Spark Streaming. 

# Efficient and Simple to Use
 
The tools and algorithms in streamDM are specifically designed for the data stream setting. Due to the large amount of data that is created – and needs to be processed – in real-time streams, such methods need to be extremely time-efficient while using very small amounts of time and memory. StreamDM includes advanced stream mining algorithms willing to be the gathering point of practical implementation and deployments for large-scale data streams.


* Ease of use. Experiments can be executed from the command-line, as in WEKA or MOA.
* No dependence on third-part libraries, specially on the linear algebra package Breeze. MLlib uses  Breeze, which depends on netlib-java, and jblas that depend on native Fortran routines. Due to license issues, netlib-java’s native libraries are not included in MLlib’s dependency set under default settings. 
* Ease of extensibility
* Advanced machine learning methods will be available as streaming decision trees, streaming Random Forests, streaming clustering methods as CluStream and StreamKM++. 


