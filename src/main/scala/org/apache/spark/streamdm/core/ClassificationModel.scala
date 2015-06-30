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

package org.apache.spark.streamdm.core

/**
 * A ClassificationModel trait defines the needed operations on any classification Model. It
 * provides methods for updating the model and for predicting the label of a
 * given Instance
 */
trait ClassificationModel extends Model {

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  def predict(instance: Example): Double

  /** Computes the probability for a given label class, given the current Model
    *
    * @param instance the Instance which needs a class predicted
    * @return the predicted probability
    */

  def prob(instance: Example): Double

}
