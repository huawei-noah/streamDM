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
 * Single label binary classification evaluator which computes the confusion
 * matrix from a stream of tuples composes of the testing Examples and doubles
 * predicted by the learners.
 */
class BasicClassificationEvaluator extends Evaluator{
  var numInstancesCorrect = 0;
  var numInstancesSeen = 0;

  /**
   * Process the result of a predicted stream of Examples and Doubles.
   *
   * @param input the input stream containing (Example,Double) tuples
   * @return a stream of String with the processed evaluation
   */
  override def addResult(input: DStream[(Example, Double)]): DStream[String] = {
    //print the confusion matrix for each batch
    val pred = ConfusionMatrix.computeMatrix(input)
    pred.map(x => {"%.3f,%.0f,%.0f,%.0f,%.0f"
      .format((x._1+x._4)/(x._1+x._2+x._3+x._4),x._1,x._2,x._3,x._4)})
  }

  /**
   * Get the evaluation result.
   *
   * @return a Double containing the evaluation result
   */
  override def getResult(): Double = 
    numInstancesCorrect.toDouble/numInstancesSeen.toDouble
}
/**
 * Helper class for computing the confusion matrix for binary classification.
 */
object ConfusionMatrix extends Serializable{
  def confusion(x: (Example,Double)):
  (Double, Double, Double, Double) = {
    val a = if ((x._1.labelAt(0)==x._2)&&(x._2==0.0)) 1.0 else 0.0
    val b = if ((x._1.labelAt(0)!=x._2)&&(x._2==0.0)) 1.0 else 0.0
    val c = if ((x._1.labelAt(0)!=x._2)&&(x._2==1.0)) 1.0 else 0.0
    val d = if ((x._1.labelAt(0)==x._2)&&(x._2==1.0)) 1.0 else 0.0
    (a,b,c,d)
  }

  def computeMatrix(input: DStream[(Example,Double)]):
  DStream[(Double,Double,Double,Double)] =
    input.map(x=>confusion(x))
      .reduce((x,y)=>(x._1+y._1,x._2+y._2,x._3+y._3,x._4+y._4))
}
