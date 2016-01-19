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
`DenseInstance` | array of `Double` | `val1,val2,...,valn`
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

A `Task` is a sequential algorithm which is tasked with connecting to a
`StreamingContext` and with processing sequential operations. This is performed
by using some building blocks, which are generally classes derived from the
following base classes:

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

Consider the (artificial) stream binary classification scenario, where
the stream consists of a stream of single label instances. The objective is to
first predict the instance using the current linear model, and then train (and
update) the model using the true label. The evaluation will be based on the
difference between the predicted label and the true label. The code listing of
the resulting `EvaluatePrequential` is:

```scala
class EvaluatePrequential extends Task {
  //Task options
  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Classifier], "SGDLearner")
  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")
  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "SocketTextStreamReader")
  val resultsWriterOption:ClassOption = new ClassOption("resultsWriter", 'w',
    "Stream writer to use", classOf[StreamWriter], "PrintStreamWriter")
  
  //Run the task
  def run(ssc:StreamingContext): Unit = {
    //Parse options and init
    val reader:StreamReader = this.streamReaderOption.getValue()
    val learner:SGDLearner = this.learnerOption.getValue()
    learner.init(reader.getExampleSpecification())
    val evaluator:Evaluator = this.evaluatorOption.getValue()
    val writer:StreamWriter = this.resultsWriterOption.getValue()

    //Parse stream and get Examples
    val instances = reader.getExamples(ssc)
    //Predict
    val predPairs = learner.predict(instances)
    //Train
    learner.train(instances)
    //Evaluate and output
    writer.output(evaluator.addResult(predPairs))
  }
}
```

First, `EvaluatePrequential` is created by extending `Task` and implementing its
`run` method. `run` takes a `StreamingContext` as an argument, and its
objective is to process the streams in this context.

The first step is the processing of the options:

```scala
    val reader:StreamReader = this.streamReaderOption.getValue()
    val learner:SGDLearner = this.learnerOption.getValue()
    learner.init(reader.getExampleSpecification())
    val evaluator:Evaluator = this.evaluatorOption.getValue()
    val writer:StreamWriter = this.resultsWriterOption.getValue()
```

Options specify the classes used for each of the components of a task; in this
case, the type of stream reader, the learner, the evaluator, and the stream
output. In addition, each class used, e.g., `SGDLearner`, can also have options,
such as the parameters needed for the algorithms. Then, for example, an
`EvaluatePrequential` parsing sparse instances and using SGD with a learning
rate of 0.001 and using hinge loss will use the command line options:

```
  EvaluatePrequential -s (SocketTextStreamReader -t sparse) -l 
    (SGDLearner -l 0.001 )
```

Then, the instances get parsed by the reader:

```scala
    val instances = reader.getExamples(ssc)
```

After the parsing, the evaluate first then train cycle is performed. In this
case, our learner is restricted to a `Classifier` so that the method `predict`
is available:

```scala
  //Predict
  val predPairs = learner.predict(instances)
  //Train
  learner.train(instances)

```

Finally, the results are output. Here, the evaluator output is combined with the
final output:

```scala
  writer.output(evaluator.addResult(predPairs))
```

## Extending StreamDM

StreamDM is designed to be easily extensible. Its purpose is to allow both users
to run it, but also developers of real-world machine learning workflows to
easily program task which are more complicated or even contain multiple layers
of learning and evaluation, and researchers to easily include new learner
algorithms in Spark Streaming.

### Adding Tasks

To define a new task, we have to extend `Task` and implement its `run` method.
We illustrate on an example of a task which writes a string to the console: 

```scala
class HelloWorldTask extends Task {

  val textOption:StringOption = new StringOption("text", 't',
    "Text to print", "Hello, World!")

  def run(ssc:StreamingContext): Unit = {
    print (textOption.getValue)
}
```

To specify the text to print, we can use a `StringOption`, and then pass it from
the command line: 

```bash
./spark "HelloWorldTask -t Bye"
```
As a general note, tasks like `EvaluatePrequential` allow to test any learner
which inherits `Classifier` - there is no need to create a task for each
classifier implemented and tested. In general, tasks should be designed so that
they allow as many options as possible at runtime without the need to compile.

## Adding Learners

To add a new learner, we only need to implement the `Learner` trait and
implement its associated methods: `init` for initializing the `Model` inside the
learner, and `train` for updating the model with the data from the stream. If
the requirements for the learner are more specific, specialized traits need to
be implemented instead. For example, the `Classifier` trait also contains a
`predict` methods which applies the model to a stream:
 
```scala
trait Classifier extends Learner with Serializable {

  /* Predict the label of the Example stream, given the current Model
   *
   * @param instance the input Example stream 
   * @return a stream of tuples containing the original instance and the
   * predicted value
   */
  def predict(input: DStream[Example]): DStream[(Example, Double)]
}

trait Learner extends Configurable  with Serializable {

  type T <: Model
  
  /**
   * Init the model based on the algorithm implemented in the learner.
   *
   * @param exampleSpecification the ExampleSpecification of the input stream.
   */
  def init(exampleSpecification: ExampleSpecification): Unit

  /** 
   * Train the model based on the algorithm implemented in the learner, 
   * from the stream of Examples given for training.
   * 
   * @param input a stream of Examples
   */
  def train(input: DStream[Example]): Unit

  /**
   * Gets the current Model used for the Learner.
   * 
   * @return the Model object used for training
   */
  def getModel: T
}
```
