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
 * A Model trait defines the needed operations on any learning Model. It
 * provides a method for updating the model.
 */
trait Model extends Serializable {

  type T <: Model

  /**
   * Update the model, depending on the Instance given for training.
   *
   * @param change the example based on which the Model is updated
   * @return the updated Model
   */
  def update(change: Example): T
}
