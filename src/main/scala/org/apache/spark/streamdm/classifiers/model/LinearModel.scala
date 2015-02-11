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

package org.apache.spark.streamdm.classifiers.model.model

import org.apache.spark.streamdm.core._

/**
 * A Model trait defines the needed operations on any learning Model. It
 * provides methods for updating the model and for predicting the label of a
 * given Instance
 */
class LinearModel(lossFunction: Loss, initialModel: Example)
  extends Model with Serializable {

  type T = LinearModel
  
  val loss = lossFunction
  val modelInstance = initialModel
  /* Update the model, depending on an Instance given for training
   *
   * @param instance the Instance based on which the Model is updated
   * @return the updated Model
   */
  override def update(changeInstance: Example): LinearModel =
    new LinearModel(loss, modelInstance.add(changeInstance)) 

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  def predict(instance: Example): Double =
    loss.predict(modelInstance.dot(instance.append(1.0)))

  /* Compute the loss of the direction of the change
   * @param instance the Instance for which the gradient is computed
   * @return an instance containging the gradients for every feature
   */
  def gradient(instance: Example): Example = {
    //compute the gradient based on the dot product, then compute the changes
    val ch = loss.gradient(instance.inst.label,
                           modelInstance.dot(instance.append(1.0)))
    instance.mapFeatures(x => ch*x)
  }

  def regularize(regularizer: Regularizer): Example = 
    modelInstance.mapFeatures(x => -regularizer.gradient(x))
}
