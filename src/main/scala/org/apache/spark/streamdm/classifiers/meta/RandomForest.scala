/*
 * Copyright (C) 2018 Télécom ParisTech LTCI lab.
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

package org.apache.spark.streamdm.classifiers.meta

import java.util.Random

import com.github.javacliparser.{ClassOption, FloatOption, IntOption}
import org.apache.spark.internal.Logging
import org.apache.spark.streamdm.classifiers.Classifier
import org.apache.spark.streamdm.classifiers.model._
import org.apache.spark.streamdm.classifiers.trees.HoeffdingTree
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.core.specification.ExampleSpecification
import org.apache.spark.streamdm.utils.Utils
import org.apache.spark.streaming.dstream._

/**
  * The streaming version of a Random Forest classifier.
  * The 2 most important aspects of this ensemble classifier are:
  * (1) inducing diversity through online bagging;
  * (2) inducing diversity through randomly selecting subsets of features for node splits;</p>
  *
  * <p>See details in:<br> Heitor Murilo Gomes, Albert Bifet, Jesse Read,
  * Jean Paul Barddal, ..., Bernhard Pfharinger, Geoff Holmes,
  * Talel Abdessalem. Adaptive random forests for evolving data stream classification.
  * In Machine Learning, DOI: 10.1007/s10994-017-5642-8, Springer, 2017.</p>
  *
  * <p>It uses the following options:
  * <ul>
  *  <li> Hyper-parameters to the base classifier (HoeffdingTree) (<b>-l</b>)
  *  <li> Size of the ensemble (<b>-s</b>)
  *  <li> The subset of features mode (how to interpret -m) (<b>-o</b>)
  *  <li> Size of the subset of randomly selected features per split (<b>-m</b>)
  *  <li> The lambda value for online bagging (<b>-a</b>)
  * </ul>
  */
class RandomForest extends Classifier with Logging {

  type T = LinearModel

  val baseClassifierOption: ClassOption = new ClassOption("baseClassifier", 'l',
    "Base Classifier to use", classOf[HoeffdingTree], "HoeffdingTree")

  val ensembleSizeOption: IntOption = new IntOption("ensembleSize", 's',
    "The number of trees in the forest.", 10, 1, Int.MaxValue)

  import com.github.javacliparser.MultiChoiceOption

  val mFeaturesModeOption = new MultiChoiceOption("mFeaturesMode", 'o',
    "Defines how m, defined by mFeaturesPerTreeSize, is interpreted. M represents the total number of features.",
    Array[String]("Specified m", "sqrt(M)+1", "M-(sqrt(M)+1)", "Percentage"),
    Array[String]("SpecifiedM", "SqrtM1", "MSqrtM1", "Percentage"), 1)

  val mFeaturesPerTreeSizeOption = new IntOption("mFeaturesPerTreeSize", 'm',
    "Number of features allowed considered for each split. Negative values corresponds to M - m", 2,
    Int.MinValue, Int.MaxValue)

  val lambdaOption = new FloatOption("lambda", 'a',
    "The lambda parameter for bagging.", 6.0, 1.0, Float.MaxValue)

  var trees: Array[HoeffdingTree] = null

  var exampleLearnerSpecification: ExampleSpecification = null

  val classifierRandom: Random = new Random()

  // The number of features to consider at every split.
  var numSplitFeatures : Int = 0

  protected val FEATURES_M = 0
  protected val FEATURES_SQRT = 1
  protected val FEATURES_SQRT_INV = 2
  protected val FEATURES_PERCENT = 3

  /** Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(exampleSpecification: ExampleSpecification): Unit = {
    exampleLearnerSpecification = exampleSpecification

    //Create the learner members of the ensemble
    val baseClassifier: HoeffdingTree = baseClassifierOption.getValue()
    val sizeEnsemble = ensembleSizeOption.getValue
    trees = new Array[HoeffdingTree](sizeEnsemble)

    // m depends on mFeaturesModeOption and mFeaturesPerTreeSizeOption
    // M represents the total number of features
    val M = exampleLearnerSpecification.in.size()
    numSplitFeatures = mFeaturesPerTreeSizeOption.getValue()

    this.mFeaturesModeOption.getChosenIndex() match {
      case FEATURES_SQRT =>
        numSplitFeatures = Math.sqrt(M).round.toInt + 1
      case FEATURES_SQRT_INV =>
        numSplitFeatures = (M - (Math.sqrt(M) + 1)).round.toInt
      case FEATURES_PERCENT =>
        // If numSplitFeatures is negative, then first find out the actual percent, i.e., 100% - numSplitFeatures.
        val percent = if (numSplitFeatures < 0) (100 + numSplitFeatures) / 100.0
        else numSplitFeatures / 100.0
        numSplitFeatures = (M * percent).round.toInt
      case FEATURES_M =>
        numSplitFeatures = numSplitFeatures
    }

    // m is negative, use size(features) + m
    if (numSplitFeatures < 0) numSplitFeatures = M + numSplitFeatures
    // Other sanity checks to avoid runtime errors.
    //  m <= 0 (m can be negative if m was negative and
    //  abs(m) > M), then use m = 1
    if (numSplitFeatures <= 0) numSplitFeatures = 1
    // m > M, then use m = M
    if (numSplitFeatures > M) numSplitFeatures = M

    logInfo("mode = %s, m = %d, M = %d".format(mFeaturesModeOption.getChosenLabel(), numSplitFeatures, M))

    // Create each tree of the forest.
    for (i <- 0 until sizeEnsemble) {
      trees(i) = Utils.copyClassifier(baseClassifier).asInstanceOf[HoeffdingTree]
      trees(i).init(exampleSpecification, numSplitFeatures)
    }

  }

  /** Train the ensemble training each of the members of the ensemble
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    for (i <- 0 until ensembleSizeOption.getValue) {
      trees(i).train(input.map(onlineSampling))
    }
    //Online Sampling with replacement
    def onlineSampling(example: Example): Example = {
      val weight = Utils.poisson(lambdaOption.getValue, classifierRandom);
      new Example(example.in, example.out, weight * example.weight)
    }
  }

  /** Builds a stream of examples and predictions based on the algorithm implemented in the classifier,
    * from the stream of instances given for testing.
    *
    * @param input a stream of examples
    * @return a stream of examples and numeric values
    */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] =
    input.map(x => (x, ensemblePredict(x)))

 /** Gets the current Model used for the Learner.
  *
  * @return the Model object used for training
  */
  override def getModel: LinearModel = null

  /** Predict the label of an example combining the predictions of the members of the ensemble
   *
   * @param example the Example which needs a class predicted
   * @return the predicted value
   */
  def ensemblePredict(example: Example): Double = {
    val sizeEnsemble = ensembleSizeOption.getValue
    val predictions: Array[Double] = new Array(sizeEnsemble)
    for (i <- 0 until sizeEnsemble) {
      predictions(i) = trees(i).getModel.asInstanceOf[ClassificationModel].predict(example)
    }
    val predictionsStr: String = predictions.map(p => p + ",").reduce((p1, p2) => p1 + p2)
    logInfo(predictionsStr)

    Utils.majorityVote(predictions, numberClasses)
  }

  def numberClasses(): Integer = {
    if (exampleLearnerSpecification == null) 2
    else exampleLearnerSpecification.out(0).range
  }
}
