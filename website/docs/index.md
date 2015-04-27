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

Spark Streaming is an extension of the core Spark API that enables scalable, high-throughput, fault-tolerant stream processing of live data streams. Data can be ingested from many sources like Kafka, Flume, Twitter, ZeroMQ, Kinesis or TCP sockets can be processed using complex algorithms expressed with high-level functions like map, reduce, join and window.

Spark Streaming receives live input data streams and divides the data into batches, which are then processed by the Spark engine to generate the final stream of results in batches.

Spark Streaming provides a high-level abstraction called discretized stream or DStream, which represents a continuous stream of data. DStreams can be created either from input data streams from sources such as Kafka, Flume, and Kinesis, or by applying high-level operations on other DStreams. Internally, a DStream is represented as a sequence of RDDs.

## Installation

To install streamDM, we only need to download the code, and compile it using:
 
{% highlight bash %}
sbt package 
{% endhighlight %}

## Running StreamDM

streamDM runs tasks. There is no need to recompile the code to run different experiments.
streamDM runs from a bash script where we can specify the task parameters. For example:

{% highlight bash %}
cd scripts
./spark "EvaluatePrequential -l SGDLearner -e BasicClassificationEvaluator 
                             -s SocketTextStreamReader"
{% endhighlight %}

In this example, we perform a prequential evaluation, or interleaved-test-then-train evaluation, using a SGDLearner and reading the data from a SocketTextStreamReader. 

In comparison with MLLib, users of streamDM have no need to compile code to change classsifiers or input streams. In an easy way, users of streamDM only need to change the parameters used in the command line. This can speed up the process of learning how to run experiments to check performance of the classifiers on different datasets.

## Machine Learning/Data Mining methods

In this first release of streamDM, currently we have implemented:

* [SGD Learner](SGD.html) (Logistic Regression,...)
* [Perceptron](SGD.html/#perceptron)
* [NB](NB.html)
* [CluStream](CluStream.html)

In the next 4 weeks we plan to release

* Hoeffding Decision Trees
* Random Forests
* Stream KM++

In the next 3 months, we plan to include 

* Frequent Itemset Miner: IncMine

## Adding new Tasks

streamDM runs tasks. To define a new task, we basically need to write a run method. This is an example of a task that writes "Hello, World!" 

{% highlight scala %}
class HelloWorldTask extends Task {

  val textOption:StringOption = new StringOption("text", 't',
    "Text to print", "Hello, World!")

  def run(ssc:StreamingContext): Unit = {
    print (textOption.getValue)
}
{% endhighlight %}

To specify the text to print, we use a string option, so that we can define it from the command line: 

{% highlight bash %}
./spark "HelloWorldTask -t Bye"
{% endhighlight %}

The run method display the string passed by the t parameter, or if there is none, it displays the default value "Hello, World!".

## Adding new classifiers

To add a new classifier, we only needs to implement the trait learner:
 
{% highlight scala %}
trait Learner extends Configurable  with Serializable {

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  def init(): Unit

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  def train(input: DStream[Example]): Unit

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  def predict(input: DStream[Example]): DStream[(Example,Double)]
}
{% endhighlight %}

