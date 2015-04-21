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

import com.github.javacliparser.IntOption
import org.apache.spark.streamdm.classifiers.Learner
import org.apache.spark.streamdm.classifiers.model.model.Model
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * Incremental Multinomial Naive Bayes learner. Builds a bayesian text classifier
 * making the naive assumption that all inputs are independent and that feature
 * values represent the frequencies with words occur. For more information
 * see,<br/> <br/> Andrew Mccallum, Kamal Nigam: A Comparison of Event Models
 * for Naive Bayes Text Classification. In: AAAI-98 Workshop on 'Learning for
 * Text Categorization', 1998.<br/> <br/>
 */

class MultinomialNaiveBayes extends Learner {

  val numClassesOption: IntOption = new IntOption("numClasses", 'c',
    "Number of Classes", 2, 2, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  val laplaceSmoothingFactorOption: IntOption = new IntOption("laplaceSmoothingFactor", 's',
    "laplace Smoothing Factor", 1, 1, Integer.MAX_VALUE)

  var model: MultinomialNaiveBayesModel = null

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(): Unit = {
    model = new MultinomialNaiveBayesModel(numClassesOption.getValue, numFeaturesOption.getValue, laplaceSmoothingFactorOption.getValue)
  }

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    input.foreachRDD {
      rdd =>
        val tmodel = rdd.aggregate(new MultinomialNaiveBayesModel(model.numClasses, model.numFeatures, model.laplaceSmoothingFactor))((mod, example) => { mod.update(example) }, (mod1, mod2) => mod1.merge(mod2))
        model.merge(tmodel)
    }
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x => (x, model.predict(x)) }
  }
}

class MultinomialNaiveBayesModel(val numClasses: Int, val numFeatures: Int, val laplaceSmoothingFactor: Int) extends Model with Serializable {
  type T = MultinomialNaiveBayesModel

  var classStatistics: Array[Int] = new Array[Int](numClasses)
  var classFeatureStatistics: Array[Array[Double]] = Array.fill(numClasses)(new Array[Double](numFeatures))

  @transient var isReady: Boolean = false

  // varialbles for prediction
  @transient var logNumberDocuments: Double = 0
  @transient var logProbability: Array[Double] = null
  @transient var logConditionalProbability: Array[Array[Double]] = null
  @transient var logNumberDocumentsOfClass: Double = 0

  override def update(instance: Instance): MultinomialNaiveBayesModel = {
    this
  }
  /* Update the model, depending on an Instance given for training
   *
   * @param instance the Instance based on which the Model is updated
   * @return the updated Model
   */
  def update(instance: Example): MultinomialNaiveBayesModel = {
    if (isReady) {
      isReady = false
    }
    classStatistics(instance.labelAt(0).toInt) += 1
    for (i <- 0 until numFeatures) {
      classFeatureStatistics(instance.labelAt(0).toInt)(i) += instance.featureAt(i)
    }
    this
  }

  /* Prepare the model
   * 
   */
  private def prepare(): Boolean = {
    if (!isReady) {
      logProbability = new Array[Double](numClasses)
      logConditionalProbability = Array.fill(numClasses)(new Array[Double](numFeatures))
      val totalnum = classFeatureStatistics.map { x => x.sum }.sum
      logNumberDocuments = math.log(totalnum + numClasses * laplaceSmoothingFactor)
      for (i <- 0 until numClasses) {
        val logNumberDocumentsOfClass = math.log(classFeatureStatistics(i).sum + numFeatures * laplaceSmoothingFactor)
        logProbability(i) = math.log(classStatistics(i) + laplaceSmoothingFactor) - logNumberDocuments
        for (j <- 0 until numFeatures) {
          logConditionalProbability(i)(j) = math.log(classFeatureStatistics(i)(j) + laplaceSmoothingFactor) - logNumberDocumentsOfClass
        }
      }
      isReady = true
    }
    return isReady
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  override def predict(instance: Example): Double = {
    if (prepare()) {
      val predictlogProbability = new Array[Double](numClasses)
      for (i <- 0 until numClasses) {
        predictlogProbability(i) = logProbability(i)
        for (j <- 0 until numFeatures) {
          predictlogProbability(i) += logConditionalProbability(i)(j) * instance.featureAt(j)
        }
      }
      argMax(predictlogProbability)
    } else 0
  }

  /* Index of maximum value of an array
   * @param Array
   * @return a Double
   */
  private def argMax(array: Array[Double]): Double = array.zipWithIndex.maxBy(_._1)._2

  /*
   * merge the statistics into current model
   * @param MultinomialNaiveBayesModel
   * @return MultinomialNaiveBayesModel
   */
  def merge(mod2: MultinomialNaiveBayesModel): MultinomialNaiveBayesModel = {

    val mergeClassStatistics = this.classStatistics.zip(mod2.classStatistics).map { case (x, y) => x + y }
    val mergeClassFeatureStatistics = this.classFeatureStatistics.zip(mod2.classFeatureStatistics).map { case (a1, a2) => a1.zip(a2).map { case (x, y) => x + y } }
    this.classStatistics = mergeClassStatistics
    this.classFeatureStatistics = mergeClassFeatureStatistics

    this
  }

}


