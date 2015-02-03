package org.apache.spark.streamdm.tasks

import com.github.javacliparser.ClassOption
import org.apache.spark.streamdm.core.DenseSingleLabelInstance
import org.apache.spark.streamdm.classifiers.{Learner, SGDLearner}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streamdm.evaluation.Evaluator

class EvaluatePrequential extends Task {

  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Learner], "SGDLearner")

  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")

  def run(ssc:StreamingContext): Unit = {

    val learner:SGDLearner = this.learnerOption.getValue()
    learner.init()
    val evaluator:Evaluator = this.evaluatorOption.getValue()

    //stream is a localhost socket stream
    val text = ssc.socketTextStream("localhost", 9999)
    //transform stream into stream of instances
    //instances come as tab delimited lines, where the first item is the label,
    //and the rest of the items are the values of the features
    val instances = text.map(
      x => new DenseSingleLabelInstance(x.split("\t").toArray.map(_.toDouble),
        x.split("\t")(0).toDouble))

    //Predict
    val predPairs = learner.predict(instances)

    //Train
    learner.train(instances)

    //Evaluate
    evaluator.addResult(predPairs)

  }
}