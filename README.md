# streamDM: Coding Guide

We will follow Spark coding style:

[https://cwiki.apache.org/confluence/display/SPARK/Spark+Code+Style+Guide](https://cwiki.apache.org/confluence/display/SPARK/Spark+Code+Style+Guide)

It is based on the Scala Style Guide:

[http://docs.scala-lang.org/style/](http://docs.scala-lang.org/style/)

It uses JavaDoc instead of ScalaDoc:

[http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html)


##Effective Scala Programming

We recommend to read the book:

[**Programming in Scala**, Second Edition A comprehensive step-by-step guide by Martin Odersky, Lex Spoon, and Bill Venners ](http://www.artima.com/shop/programming_in_scala)

and follow the Twitter Effective Scala guidelines:

[http://twitter.github.io/effectivescala/](http://twitter.github.io/effectivescala/)

# streamDM: Compiling the code

Use 

```
#!bash

sbt package
```
# streamDM: Running the app

You need to run two scripts in two different terminals.

*  In the first terminal: create the dataset syn.dat (only the first time)
```
#!bash

cd scripts/instance_server
./generate_dataset.py
```
* And then send instances into port 9999, localhost

```
#!bash

cd scripts/instance_server
./server.py
```
* In the second terminal: use Spark to learn the model and output accuracy:

```
#!bash
cd scripts
./spark
```
