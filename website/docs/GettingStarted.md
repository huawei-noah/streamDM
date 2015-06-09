---
layout: page
title: StreamDM: Quick Start Guide
image:
  feature: screen_network.png
---

# Quick Start Guide

The purpose of this document is to provide a quick entry point for users
desiring to quickly run the a StreamDM task. We describe how StreamDM can be
compiled, and how a quick task can be run.

The basic requirement for running the example in this documents is to have Spark
1.3 installed. The example works best on a Linux/Unix machine.

# Compiling The Code

In the main folder of StreamDM, the following command generates the package
needed to run StreamDM: 
{% highlight bash %}
sbt package
{% endhighlight %}

# Running The Task

You need to run two scripts in two different terminals.

*  In the first terminal: create the dataset syn.dat (only the first time)
{% highlight bash %}

cd scripts/instance_server
./generate_dataset.py
{% endhighlight %}

* And then send instances into port 9999, localhost
{% highlight bash %}
cd scripts/instance_server
./server.py
{% endhighlight %}

* In the second terminal: use Spark to learn the model and output accuracy:

{% highlight bash %}
cd scripts
./spark
{% endhighlight %}
