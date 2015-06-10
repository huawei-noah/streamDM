---
layout: page
title: StreamDM: Quick Start Guide
image:
  feature: screen_network.png
---
The purpose of this document is to provide a quick entry point for users
desiring to quickly run the a StreamDM task. We describe how StreamDM can be
compiled, and how a quick task can be run.

The basic requirement for running the example in this documents is to have Spark
1.3 installed. The example works best on a Linux/Unix machine.

## Compiling The Code

In the main folder of StreamDM, the following command generates the package
needed to run StreamDM:

{% highlight bash %}
sbt package
{% endhighlight %}

## Running The Task

The task that is run in this example is the EvaluatePrequential. By default, the
task connects to a socket open on the localhost at port 9999 which sends dense
instances as a stream. Then a linear binary classifier is trained by using
StochasticGradientDescent and the predictions are evaluted by outputting the
confusion matrix.

The example can be run by executing the following steps:

* In the first terminal: create the dataset syn.dat (only needed once); this
  script will generate a file containing dense instances of 3 features:

{% highlight bash %}
cd scripts/instance_server
./generate_dataset.py
{% endhighlight %}

* After, in one terminal, start the server sending instances in syn.dat into
  port 9999 on localhost; this stream will be read by the task:

{% highlight bash %}
cd scripts/instance_server
./server.py
{% endhighlight %}

* In another terminal, use the provided spark script to run the task (after
  modifying the SPARK_HOME variable with the folder of your Spark installation:

{% highlight bash %}
cd scripts
./spark.sh
{% endhighlight %}

* [Optional] It is advisable to separate the standard and the error output, for
  better readability:

{% highlight bash %}
cd scripts
./spark.sh 1>results.txt 2>debug.log
{% endhighlight %}

The standard output will contain a confusion matrix aggregating the prediction
results for every Spark RDD in the stream.


