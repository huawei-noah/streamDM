---
layout: page
title: StreamDM Programming Guide
image:
  feature: screen_network.png
---

Everything in StreamDM is designed around tasks, which describe the flow of a
streaming data mining or machine learning algorithm. In a
nutshell, streaming data is read and parsed into the StreamDM internal
representation, passed through learners, evaluated and then output to various
places, such as console, files, or as streams for other tasks. 

## Basic Data Structure

StreamDM processes multi-dimensional vectors, called _instances_, which are
implemented as classes derived from a general class `Instance`. Depending on the
implementations of the underlying data structures, and `Instance` can be:

__Instance Type__ | __Data Structure__ | __Format__
--- | --- | ---
`DenseInstance` | array of Double | `val1,val2,...,valn`
`SparseInstance` | two arrays: one for indexes, one for values | `idx1:val1,idx2:val2,...`
`TextInstance` | map of key-value tuples; allows non-numeric keys | `key1:val1,key2:val2,...`
`NullInstance` | N/A | N/A

`Instance` provides useful operations for use in a `Learner`, such as the
`dot` and `distanceTo` operations, but also `map` and `reduce` operations. Note
that an `Instance` is always immutable; every operation returns a new `Instance`
with the modifications. For full details, please refer to the `Instance`
specification in the API documentation.

The input/output data structure which is sent via `DStream` is the `Example`.
This data structure  wraps input and output instances, along with a number
representing its weight. The class signature is specified as:

```scala
class Example(inInstance: Instance, outInstance: Instance = new NullInstance, 
              weightValue: Double=1.0) 
```

Its format is the following (note that weight and the output instance are
optional):

```
<input_instance> [<output_instance>] [<weight>]  
```
The `Example.parse` method allows StreamDM to create objects of type `Example`
from text lines in the stream.

By default, every value in each instance is a Double. In cases where the values
have different types of values (for example, discrete integers) a helper data
structure `ExampleSpecification` is used. 

An `ExampleSpecification` is used in special cases: classifiers such as decision
trees and Naive Bayes, which need to know the type of each feature in the
instance. In the current implementation, we support two types of features,
numeric and discrete. Numeric values are the default, and discrete features are
strings (such as colors *Red*, *Green*, *Blue*, etc.) which are internally
represented as doubles, for space efficiency and compatibility with the
`Instance` classes.

This information is stored in an `ExampleSpecification` object, which, similarly
to `Example`, contains two `InstanceSpecification` objects for the input and
output instances. Each `InstanceSpecification` stores the name of the features,
and, for the discrete features, their original string descriptions in an
associated `FeatureSpecification` object.

## Task Building Blocks

A `Task` is a sequential algorithm which is tasked to connecting to
`StreamingContext` and process sequential operations using as building blocks
classes derived from the following base classes:

__Base Class__ | __Purpose__ 
--- | ---
`StreamReader` | read and parse `Example` and create a stream
`Learner` | provides the `train` method from an input stream
`Model` | data structure and set of methods used for `Learner`
`Evaluator` | evaluation of predictions
`StreamWriter` | output of streams 

The base classes above can also be extended for more specific use cases. For
instance, `Learner` is currently extended to `Classifier`, which provides a
`predict` method, and a `Clusterer`, which provides an `assign` method.

A `Task` will also contain a set of options, which use the
[JavaCLIParser](https://github.com/abifet/javacliparser/) library. These options
allow to specify what types of learners, evaluators, writers, and readers are to
be used, at *runtime*, without the need to re-compile the task.

An illustration on how a `Task` can be programmed by using a combination of the
above building block is the `EvaluatePrequential` example task, present in the
source code of StreamDM.

### Case Study: EvaluatePrequential

We begin by showing the full listing of `EvaluatePrequential`:

```scala
  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Classifier], "SGDLearner")
  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")
  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "SocketTextStreamReader")
  val resultsWriterOption:ClassOption = new ClassOption("resultsWriter", 'w',
    "Stream writer to use", classOf[StreamWriter], "PrintStreamWriter")


  def run(ssc:StreamingContext): Unit = {

    val reader:StreamReader = this.streamReaderOption.getValue()
    val learner:SGDLearner = this.learnerOption.getValue()
    learner.init(reader.getExampleSpecification())
    val evaluator:Evaluator = this.evaluatorOption.getValue()
    val writer:StreamWriter = this.resultsWriterOption.getValue()

    val instances = reader.getExamples(ssc)

    //Predict
    val predPairs = learner.predict(instances)
    //Train
    learner.train(instances)
    //Evaluate
    writer.output(evaluator.addResult(predPairs))
  }
```
