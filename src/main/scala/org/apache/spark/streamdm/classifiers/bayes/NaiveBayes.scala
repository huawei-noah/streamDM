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

import com.github.javacliparser.{ ClassOption, FloatOption, IntOption }
import org.apache.spark.streamdm.classifiers.model.model.Model
import org.apache.spark.streamdm.classifiers.IncrementalLearner
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * Naive Bayes incremental learner.
 *
 * Performs classic bayesian prediction while making naive assumption that
 * all inputs are independent. Naive Bayes is a classiﬁer algorithm known
 * for its simplicity and low computational cost. Given n different classes, the
 * trained Naive Bayes classiﬁer predicts for every unlabelled instance I the
 * class C to which it belongs with high accuracy.
 */

class NaiveBayesMultinomial extends IncrementalLearner{

  val numClassesOption: IntOption = new IntOption("numClasses", 'c',
    "Number of Classes", 2, 2, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  val laplaceSmoothingFactorOption: IntOption = new IntOption("laplaceSmoothingFactor", 's',
    "laplace Smoothing Factor", 1, 1, Integer.MAX_VALUE)

  var model: NaiveBayesModel = null
  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(): Unit = {
    model = new DenseNaiveBayesMultinomialModel(numClassesOption.getValue, numFeaturesOption.getValue, laplaceSmoothingFactorOption.getValue)
  }

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    input.map { model.train(_) }
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x => (x, model.predict(x)) }
  }

  /* run for evaluation, first predict the label of the Instance, then update the model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def evaluate(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x =>
      {
        val predict = model.predict(x)
        model.train(x)
        (x, model.predict(x))
      }
    }
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

class DenseNaiveBayesMultinomialModel extends NaiveBayesModel with Serializable {
  type T = DenseNaiveBayesMultinomialModel

  var numClasses: Int = 0
  var numFeatures: Int = 0
  var laplaceSmoothingFactor: Int = 1
  var labels: Array[Int] = null
  var pointNum: Int = 0
  var aggregate: Array[Array[Double]] = null
  var outlierNum: Int = 0

  def this(numClasses: Int, numFeatures: Int, laplaceSmoothingFactor: Int) {
    this()
    this.numClasses = numClasses
    this.numFeatures = numFeatures
    this.laplaceSmoothingFactor = laplaceSmoothingFactor
    labels = new Array[Int](numClasses)
    aggregate = Array.fill(numClasses)(new Array[Double](numFeatures))
  }

  /* train the model, depending on the Instance given for training
   *
   * @param changeInstance the Instance based on which the Model is updated
   * @return Unit
   */
  override def train(changeInstance: Example): Unit = {
    labels(changeInstance.labelAt(0).toInt) += 1
    for (i <- 0 until numFeatures) {
      aggregate(changeInstance.labelAt(0).toInt)(i) += changeInstance.featureAt(i)
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
    val logNumberDocuments = math.log(pointNum + numClasses * laplaceSmoothingFactor)
    val logProbability = new Array[Double](numClasses)
    val logConditionalProbability = Array.fill(numClasses)(new Array[Double](numFeatures))
    for (i <- 0 until numClasses) {
      val logNumberDocumentsOfClass = math.log(aggregate(i).sum + numFeatures * laplaceSmoothingFactor)
      logProbability(i) = math.log(labels(i) + laplaceSmoothingFactor) - logNumberDocuments
      for (j <- 0 until numFeatures) {
        logConditionalProbability(i)(j) = math.log(aggregate(i)(j) + laplaceSmoothingFactor) - logNumberDocumentsOfClass
        logProbability(i) += logConditionalProbability(i)(j) + instance.featureAt(j)
      }
    }
    logProbability
  }

}

object NaiveBayes {

}