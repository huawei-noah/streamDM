---
layout: page
title: SampleDataWriter
image:
  feature: screen_network.png
---

## SampleDataWriter

Calls data generators to create sample data for simulation or test.
Runs command line with parameters:
  {% highlight bash %}
  ./generateData.sh "FileWriter -n 1000 -f ./HPData.10.L -g (HyperplaneGenerator -k 100 -f 10)"
  {% endhighlight %}

### Parameters:
 * Chunk number (-n)
 * File Name (-f)
 * Generator (-g)
