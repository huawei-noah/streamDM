---
layout: page
title: Getting Started
image:
  feature: screen_network.png
---

# streamDM: Compiling the code

Use 

{% highlight bash %}
sbt package
{% endhighlight %}

# streamDM: Running the app

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
