---
layout: page
title: StreamDM Quick Start Guide
image:
  feature: screen_network.png
---
The purpose of this document is to provide a quick entry point for users
desiring to quickly run a StreamDM task. We describe how StreamDM can be
compiled, and how a quick task can be run.

The basic requirement for running the example in this documents is to have Spark
1.4 installed. The example works best on a Linux/Unix machine.

## Compiling The Code

In the main folder of StreamDM, the following command generates the package
needed to run StreamDM:

```bash
sbt package
```

## Running The Task

The task that is run in this example is the EvaluatePrequential. By default, the
task connects to a socket open on the localhost at port 9999 which sends dense
instances as a stream. Then a linear binary classifier is trained by using
StochasticGradientDescent and the predictions are evaluted by outputting the
confusion matrix.

The example can be run by executing in commend line:

* In the terminal, use the provided spark script to run the task (after
  modifying the `SPARK_HOME` variable with the folder of your Spark installation):

  {% highlight bash %}
  cd scripts
  ./spark.sh
  {% endhighlight %}

* It is possible to add command-line options to specify task, learner, and evaluation parameters:
 
  {% highlight bash %}
  cd scripts
  ./spark.sh "EvaluatePrequential -l (SGDLearner -l 0.01 -o LogisticLoss -r ZeroRegularizer) –s (FileReader –k 100 –d 60 –f ../data/mydata)"
  {% endhighlight %}

* [Optional] It is advisable to separate the standard and the error output, for
  better readability:

  {% highlight bash %}
  cd scripts
  ./spark.sh 1>results.txt 2>debug.log
  {% endhighlight %}

The standard output will contain a confusion matrix aggregating the prediction
results for every Spark RDD in the stream.

Four [data generators](http://huawei-noah.github.io/streamDM/docs/generators.html) can generate sample data 
by  [SampleDataWriter](http://huawei-noah.github.io/streamDM/docs/SampleDataWriter.html):

* In the terminal, use the provided sample data generator script to generate sample data (after
  modifying the `SPARK_HOME` variable with the folder of your Spark installation
  and setting the correct jar files):

  {% highlight bash %}
  cd scripts
  ./generateData.sh "FileWriter -n 1000 -f ../sampledata/mysampledata -g (HyperPlaneGenerator -k 100 -f 10)"
  {% endhighlight %}
