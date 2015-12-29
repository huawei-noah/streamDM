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

package org.apache.spark.streamdm.classifiers

import com.github.javacliparser.ClassOption
import org.apache.spark.streamdm.classifiers.model._
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._
import org.apache.spark.streamdm.utils.Utils
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * The MultiClassLearner trains a model for each class.
 * The class predicted is the one that its model predicts with highest
 * confidence.
 *
 * <p>It uses the following option:
 * <ul>
 *  <li> Base Classifier to use (<b>-l</b>)
 * </ul>
 */

class MultiClassLearner extends Classifier {

  type T = LinearModel

  val baseClassifierOption: ClassOption = new ClassOption("baseClassifier", 'l',
    "Base Classifier to use", classOf[Classifier], "SGDLearner")

  var classifiers: Array[Classifier] = null

  var exampleLearnerSpecification: ExampleSpecification = null

  var sizeEnsemble: Int = 0

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(exampleSpecification: ExampleSpecification): Unit = {
    exampleLearnerSpecification = exampleSpecification

    //Create the learner members of the ensemble
    val baseClassifier: Classifier = baseClassifierOption.getValue()
    sizeEnsemble = exampleSpecification.outputFeatureSpecification(0).range
    classifiers = new Array[Classifier](sizeEnsemble)

    for (i <- 0 until sizeEnsemble) {
      classifiers(i) = Utils.copyClassifier(baseClassifier)
      classifiers(i).init(exampleSpecification)
    }

  }

  /* Train the model using the members of the ensemble
     *
     * @param input a stream of instances
     * @return the updated Model
     */
  override def train(input: DStream[Example]): Unit = {
    for (labelClass <- 0 until sizeEnsemble) {
      classifiers(labelClass).train(input.map(convertInstance))

      def convertInstance(example: Example): Example = {
        new Example(example.in, new DenseInstance(Array(if (example.labelAt(0) == labelClass) 1 else 0)))
      }
    }
  }

  /* Builds a stream of examples and predictions based on the algorithm implemented in the classifier,
    * from the stream of instances given for testing.
    *
    * @param input a stream of examples
    * @return a stream of examples and numeric values
    */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] =
    input.map(x => (x, ensemblePredict(x)))

  /* Gets the current Model used for the Learner.
  *
  * @return the Model object used for training
  */
  override def getModel: LinearModel = null

  /* Predict the label of an example combining the predictions of the members of the ensemble
   *
   * @param example the Example which needs a class predicted
   * @return the predicted value
   */
  def ensemblePredict(example: Example): Double = {
    val predictions: Array[Double] = new Array(sizeEnsemble)
    for (labelClass <- 0 until sizeEnsemble) {
      predictions(labelClass) = classifiers(labelClass).getModel.asInstanceOf[ClassificationModel].prob(convertExample(example))

      def convertExample(example: Example): Example = {
        new Example(example.in, new DenseInstance(Array(if (example.labelAt(0) == labelClass) 1 else 0)))
      }
    }
    Utils.argmax(predictions)
  }


}
