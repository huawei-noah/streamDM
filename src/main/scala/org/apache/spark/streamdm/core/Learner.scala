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

import com.github.javacliparser.Configurable
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * A Learner trait defines the needed operations on any learner implemented. It
 * provides methods for training the model and for predicting the labels for a
 * stream of Instance RDDs.
 */
trait Learner extends Configurable  with Serializable {

  type T <: Model
  
  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  def init: Unit

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  def train(input: DStream[Example]): Unit

  /* Gets the current Model used for the Learner.
   * 
   * @return the Model object used for training
   */
  def getModel: T
}
