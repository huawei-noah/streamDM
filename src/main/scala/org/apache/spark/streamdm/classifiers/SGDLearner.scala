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

import com.github.javacliparser.{ClassOption, FloatOption, IntOption}
import org.apache.spark.streamdm._
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.model.model._
import org.apache.spark.streaming.dstream._

/**
 * The SGDLearner trains a LinearModel using the stochastic gradient descent
 * algorithm. At construction, the type of loss function, the lambda learning
 * reate parameter, and the number of features need to be specified
 */
class SGDLearner extends Learner {

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  val lambdaOption: FloatOption = new FloatOption("lambda", 'l',
    "Lambda", .1, 0, Float.MaxValue)

  val lossFunctionOption:ClassOption = new ClassOption("lossFunction", 'o',
    "Loss function to use", classOf[Loss], "LogisticLoss")


  var model: LinearModel = null

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(): Unit = {
    model = new LinearModel(lossFunctionOption.getValue(),
      new Example(new DenseSingleLabelInstance(
        Array.fill[Double](numFeaturesOption.getValue + 1)(0.0), 0.0)))

  }

  /* Train the model using stochastic gradient descent.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    //train the changes
    //first, compute the gradient
    //then, add the gradients together
    //finally, apply lambda
    val changes = input.map(x => model.gradient(x)).
      reduce((x, y) => x.add(y)).
      map(x => x.mapFeatures(x => lambdaOption.getValue * x))
    //apply the final changes to the new model
    changes.foreachRDD(rdd => {
      model = model.update(rdd.first())
    })
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] =
    input.map(x => (x, model.predict(x)))
}
