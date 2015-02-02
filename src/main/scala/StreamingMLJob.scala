import java.lang.System._

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream._
import org.apache.spark.streaming.StreamingContext._

import org.apache.spark.streamdm._
import org.apache.spark.streamdm.input._
import org.apache.spark.streamdm.model._
import org.apache.spark.streamdm.regression._

import com.github.javacliparser.{FloatOption, ClassOption, IntOption, Configurable}

import java.io._

object StreamingMLJob {

  def main(args: Array[String]) {

    //configuration and initialization of model
    val conf = new SparkConf().setAppName("StreamingSGD")
    conf.setMaster("local[2]")

    val ssc = new StreamingContext(conf, Seconds(10))

    // Run Task
    //val string = args.mkString(" ")
    val string = "EvaluatePrequential"
    val task:Task = ClassOption.cliStringToObject(string, classOf[Task], null)
    task.run(ssc)

    //start the loop
    ssc.start()
    ssc.awaitTermination()
  }
}


//Tasks
abstract class Task extends Configurable with Serializable{
  def run(ssc:StreamingContext): Unit
}

class EvaluatePrequential extends Task {

  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Learner], "regression.SGDLearner")

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

//Evaluator
abstract class Evaluator extends Configurable with Serializable{
  def addResult(input: DStream[(DenseSingleLabelInstance, Double)])
  def getResult():Double
}
class BasicClassificationEvaluator extends Evaluator{
  var numInstancesCorrect = 0;
  var numInstancesSeen = 0;

  def addResult(input: DStream[(DenseSingleLabelInstance, Double)]): Unit = {
    //print the confusion matrix for each batch
    val pred = ConfusionMatrix.computeMatrix(input)
    pred.foreachRDD(rdd => {
      rdd.foreach(x => {println("Acc: %.2f, Matrix: %.0f,%.0f,%.0f,%.0f"
        .format((x._1+x._4)/(x._1+x._2+x._3+x._4),x._1,x._2,x._3,x._4))})
    })
  }

  def getResult():Double = {
    numInstancesCorrect.toDouble/numInstancesSeen.toDouble
  }
}

object ConfusionMatrix extends Serializable{
  def confusion(x: (DenseSingleLabelInstance,Double)):
  (Double, Double, Double, Double) = {
    val a = if ((x._1.label==x._2)&&(x._2==0.0)) 1.0 else 0.0
    val b = if ((x._1.label!=x._2)&&(x._2==0.0)) 1.0 else 0.0
    val c = if ((x._1.label!=x._2)&&(x._2==1.0)) 1.0 else 0.0
    val d = if ((x._1.label==x._2)&&(x._2==1.0)) 1.0 else 0.0
    (a,b,c,d)
  }

  def computeMatrix(input: DStream[(DenseSingleLabelInstance,Double)]):
  DStream[(Double,Double,Double,Double)] =
    input.map(x=>confusion(x))
      .reduce((x,y)=>(x._1+y._1,x._2+y._2,x._3+y._3,x._4+y._4))
}
