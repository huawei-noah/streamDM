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

import com.github.javacliparser.FloatOption
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streaming.dstream.DStream

/**
 * Single label binary classification evaluator which computes the confusion
 * matrix from a stream of tuples composed of testing Examples and doubles
 * predicted by the learners.
 */
class BasicClassificationEvaluator extends Evaluator{

  val betaOption = new FloatOption("beta", 'b',
    "Beta value for fbeta-score calculation.", 1.0, Double.MinValue, Double.MaxValue)

  var numInstancesCorrect = 0;
  var numInstancesSeen = 0;

  /**
   * Process the result of a predicted stream of Examples and Doubles.
   *
   * @param input the input stream containing (Example,Double) tuples
   * @return a stream of String with the processed evaluation
   */
  override def addResult(input: DStream[(Example, Double)]): DStream[String] = {
    val confusionMatrix = ConfusionMatrix.computeMatrix(input)
    confusionMatrix.map(calculateMetrics)
  }

  /**
    * Helper function to calculate several evaluation metrics based on a confusion matrix
    * @param confMat
    * @return
    */
  def calculateMetrics(confMat : Map[String,Double] ): String = {
    val accuracy = (confMat{"tp"}+confMat{"tn"})/(confMat{"tp"}+confMat{"tn"}+confMat{"fp"}+confMat{"fn"})
    val recall = confMat{"tp"} / (confMat{"tp"}+confMat{"fn"})
    val precision = confMat{"tp"} / (confMat{"tp"}+confMat{"fp"})
    val specificity = confMat{"tn"} / (confMat{"tn"}+confMat{"fp"})
    val f_beta_score = (1 + scala.math.pow(this.betaOption.getValue(),2)) * ((precision * recall) /
      ((scala.math.pow(this.betaOption.getValue(),2) * precision) + recall))

     "%.3f,%.3f,%.3f,%.3f,%.3f,%.0f,%.0f,%.0f,%.0f".format(accuracy, recall, precision, f_beta_score, specificity,
        confMat{"tp"}, confMat{"fn"}, confMat{"fp"}, confMat{"tn"})
  }

  override def header(): String = {
    "Accuracy,Recall,Precision,F(beta=%.1f)-score,Specificity,TP,FN,FP,TN".format(this.betaOption.getValue())
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
  Map[String, Double] = {
    val tp = if ((x._1.labelAt(0)==x._2)&&(x._2==0.0)) 1.0 else 0.0
    val fn = if ((x._1.labelAt(0)!=x._2)&&(x._2==0.0)) 1.0 else 0.0
    val fp = if ((x._1.labelAt(0)!=x._2)&&(x._2==1.0)) 1.0 else 0.0
    val tn = if ((x._1.labelAt(0)==x._2)&&(x._2==1.0)) 1.0 else 0.0
    Map("tp" -> tp, "fn" -> fn, "fp" -> fp, "tn" -> tn)
  }

  def computeMatrix(input: DStream[(Example,Double)]):
  DStream[Map[String, Double]] =
      input.map(x => confusion(x)).reduce( (x,y) =>
        Map("tp" -> (x{"tp"} + y{"tp"}),
            "fn" -> (x{"fn"} + y{"fn"}),
            "fp" -> (x{"fp"} + y{"fp"}),
            "tn" -> (x{"tn"} + y{"tn"})))
}
