import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream._
import org.apache.spark.streaming.StreamingContext._

import org.apache.spark.streamdm._
import org.apache.spark.streamdm.input._
import org.apache.spark.streamdm.model._
import org.apache.spark.streamdm.regression._

import java.io._

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
  
object StreamingMLJob {


  def main(args: Array[String]) {
    val numFeatures = 3
    val lambda = 0.1
    //configuration and initialization of model
    val conf = new SparkConf().setAppName("StreamingSGD")
    conf.setMaster("local[2]")
 
    val loss = new LogisticLoss() 
    val learner = new SGDLearner(loss, lambda, numFeatures)
    
    val ssc = new StreamingContext(conf, Seconds(10))
    //stream is a localhost socket stream
    val text = ssc.socketTextStream("localhost", 9999)
    //transform stream into stream of instances
    //instances come as tab delimited lines, where the first item is the label,
    //and the rest of the items are the values of the features
    val instances = text.map(
      x => new DenseSingleLabelInstance(x.split("\t").toArray.map(_.toDouble),
        x.split("\t")(0).toDouble))
    
    //predict - learn
    val predPairs = learner.predict(instances)
    learner.train(instances)
    
    //print the confusion matrix for each batch
    val pred = ConfusionMatrix.computeMatrix(predPairs)
    pred.foreachRDD(rdd => {
      rdd.foreach(x => {println("%.0f,%.0f,%.0f,%.0f"
        .format(x._1,x._2,x._3,x._4))})
    })
    
    //start the loop
    ssc.start()
    ssc.awaitTermination()
  }
}
