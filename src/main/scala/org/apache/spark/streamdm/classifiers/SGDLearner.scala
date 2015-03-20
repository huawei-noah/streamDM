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
import org.apache.spark.Logging
import org.apache.spark.streamdm._
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.model.model._
import org.apache.spark.streaming.dstream._

/**
 * The SGDLearner trains a LinearModel using the stochastic gradient descent
 * algorithm. The type of loss function, the lambda learning
 * reate parameter, and the number of features need to be specified in the
 * associated Task configuration file.
 */
class SGDLearner extends Learner {

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  val lambdaOption: FloatOption = new FloatOption("lambda", 'l',
    "Lambda", .01, 0, Float.MaxValue)

  val lossFunctionOption: ClassOption = new ClassOption("lossFunction", 'o',
    "Loss function to use", classOf[Loss], "LogisticLoss")

  val regularizerOption:ClassOption = new ClassOption("regularizer",
    'r', "Regularizer to use", classOf[Regularizer], "ZeroRegularizer")

  val regularizerParameter: FloatOption = new FloatOption("regParam", 'p',
    "Regularization parameter", .001, 0, Float.MaxValue)


  var model: LinearModel = null
  val regularizer: Regularizer = regularizerOption.getValue()
  val loss: Loss = lossFunctionOption.getValue()

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(): Unit = {
    model = new LinearModel(loss,
      new Example(new DenseSingleLabelInstance(
        Array.fill[Double](numFeaturesOption.getValue + 1)(0.0), 0.0)),
      numFeaturesOption.getValue)
  }

  /* Train the model using stochastic gradient descent.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    input.foreachRDD(rdd=> {
      val chModels = rdd.aggregate(
        (new LinearModel(loss,model.modelInstance,model.numFeatures),0.0))(
        (mod,inst) => {
          val grad = mod._1.gradient(inst)
          val reg = mod._1.regularize(regularizer).mapFeatures(f =>
              f*regularizerParameter.getValue)
          val change = grad.add(reg).mapFeatures(f => f*lambdaOption.getValue)
          (mod._1.update(change),1.0)
        },
        (mod1,mod2) =>
          (mod1._1.update(mod2._1.modelInstance),mod1._2+mod2._2)
        )
      if(chModels._2>0)
        model = new LinearModel(loss,
          chModels._1.modelInstance.mapFeatures(f => f/chModels._2),
          model.numFeatures)
    })
    /*
    //train the changes
    //first, compute the gradient
    //then, add the gradients together
    //finally, apply lambda
    val changes = input.map(inst => (model.gradient(inst),1.0)).
      reduce((grad1, grad2) => (grad1._1.add(grad2._1), grad1._2+grad2._2)).
      map(sumGrad => sumGrad._1.mapFeatures(f => lambdaOption.getValue *
        (f/sumGrad._2)))
    //apply the final changes to the new model in two steps:
    //- first, add the regularizer (for now, weighted by lambda
    //- second, apply the change vector to the model
    changes.foreachRDD(rdd => {
      model =
        model.update(rdd.first().add(model.regularize(regularizer)                           mapFeatures(f => lambdaOption.getValue*
                                       regularizerParameter.getValue*f)))
    })
    */
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] =
    input.map(x => (x, model.predict(x)))
}
