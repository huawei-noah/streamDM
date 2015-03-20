/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.evaluation

import java.io.Serializable

import org.apache.spark.streamdm.core.Example
import org.apache.spark.streaming.dstream.DStream

/**
 * Classification evaluator that performs basic incremental evaluation.
 *
 */
class BasicClassificationEvaluator extends Evaluator{
  var numInstancesCorrect = 0;
  var numInstancesSeen = 0;

  def addResult(input: DStream[(Example, Double)]): Unit = {
    //print the confusion matrix for each batch
    val pred = ConfusionMatrix.computeMatrix(input)
    pred.foreachRDD(rdd => {
      rdd.foreach(x => {println("%.3f,%.0f,%.0f,%.0f,%.0f"
        .format((x._1+x._4)/(x._1+x._2+x._3+x._4),x._1,x._2,x._3,x._4))})
    })
  }

  def getResult():Double = {
    numInstancesCorrect.toDouble/numInstancesSeen.toDouble
  }
}
/**
 * Confusion matrix for binary classification.
 *
 */
object ConfusionMatrix extends Serializable{
  def confusion(x: (Example,Double)):
  (Double, Double, Double, Double) = {
    val a = if ((x._1.inst.label==x._2)&&(x._2==0.0)) 1.0 else 0.0
    val b = if ((x._1.inst.label!=x._2)&&(x._2==0.0)) 1.0 else 0.0
    val c = if ((x._1.inst.label!=x._2)&&(x._2==1.0)) 1.0 else 0.0
    val d = if ((x._1.inst.label==x._2)&&(x._2==1.0)) 1.0 else 0.0
    (a,b,c,d)
  }

  def computeMatrix(input: DStream[(Example,Double)]):
  DStream[(Double,Double,Double,Double)] =
    input.map(x=>confusion(x))
      .reduce((x,y)=>(x._1+y._1,x._2+y._2,x._3+y._3,x._4+y._4))
}
