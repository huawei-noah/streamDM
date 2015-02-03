package org.apache.spark.streamdm.evaluation

import java.io.Serializable

import org.apache.spark.streamdm.core.DenseSingleLabelInstance
import org.apache.spark.streaming.dstream.DStream

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
