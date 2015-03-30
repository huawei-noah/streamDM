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

package org.apache.spark.streamdm.classifiers.bayes

import org.apache.spark.streamdm.classifiers.model.Model
import org.apache.spark.streamdm.classifiers.Learner
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

class NaiveBayes(val model: NaiveBayesModel) extends Learner with Serializable {
  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  def init(): Unit = {}

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  def train(input: DStream[Example]): Unit = {
    input.map { model.train(_) }
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  def predict(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x => (x, model.predict(x)) }
  }
}

trait NaiveBayesModel extends Model with Serializable {
  /* train the model, depending on the Instance given for training
   *
   * @param changeInstance the Instance based on which the Model is updated
   * @return Unit
   */
  def train(changeInstance: Example): Unit
}

class DenseNaiveBayesModel extends NaiveBayesModel with Serializable {
  type T = DenseNaiveBayesModel

  var range: Int = 0
  var featurelen: Int = 0
  var lamda: Int = 1
  var labels: Array[Int] = null
  var pointNum: Int = 0
  var aggregate: Array[Array[Double]] = null
  var outlierNum: Int = 0

  def this(range: Int, featurelen: Int, lambda: Int) {
    this()
    this.range = range
    this.featurelen = featurelen
    this.lamda = lambda
    labels = new Array[Int](range)
    aggregate = Array.fill(range)(new Array[Double](featurelen))
  }

  /* train the model, depending on the Instance given for training
   *
   * @param changeInstance the Instance based on which the Model is updated
   * @return Unit
   */
  def train(changeInstance: Example): Unit = {
    labels(changeInstance.labelAt(0).asInstanceOf[Int]) += 1
    for (i <- 0 until featurelen) {
      aggregate(changeInstance.labelAt(0).asInstanceOf[Int])(i) += changeInstance.featureAt(i)
    }
  }

  /* Update the model, depending on an Instance given for training
   *
   * @param instance the Instance based on which the Model is updated
   * @return the updated Model
   */
  override def update(changeInstance: Example): T = {
    train(changeInstance)
    this
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  def predict(instance: Example): Double = {
    argMax(predictPi(instance))
  }

  /* argMax of an array
   * @param Array
   * @return a Double
   */
  private def argMax(datas: Array[Double]): Double = {
    var argmax: Double = -1
    var v = Double.MinValue
    for (i <- 0 until datas.length) {
      if (datas(i) > v) {
        argmax = i
        v = datas(i)
      }
    }
    argmax
  }

  /* Predict the probabilities of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double Array representing the probabilities
   */
  private def predictPi(instance: Example): Array[Double] = {
    val piLogDemon = math.log(pointNum + range * lamda)
    val pi = new Array[Double](range)
    val theta = Array.fill(range)(new Array[Double](featurelen))
    for (i <- 0 until range) {
      val thetalogDemon = math.log(aggregate(i).sum + featurelen * lamda)
      pi(i) = math.log(labels(i) + lamda) - piLogDemon
      for (j <- 0 until featurelen) {
        theta(i)(j) = math.log(aggregate(i)(j) + lamda) - thetalogDemon
        pi(i) += theta(i)(j) + instance.featureAt(j)
      }
    }
    pi
  }

}

object NaiveBayes {

}
