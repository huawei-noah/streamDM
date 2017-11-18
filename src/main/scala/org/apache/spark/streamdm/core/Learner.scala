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

import org.apache.spark.streaming.dstream._
import com.github.javacliparser.Configurable
import org.apache.spark.streamdm.core.specification.ExampleSpecification


/**
 * A Learner trait defines the needed operations for any learner algorithm
 * implemented. It provides methods for training the model for a stream of
 * Example RDDs.
 * Any Learner will contain a data structure derived from Model.
 */
trait Learner extends Configurable  with Serializable {

  type T <: Model
  
  /**
   * Init the model based on the algorithm implemented in the learner.
   *
   * @param exampleSpecification the ExampleSpecification of the input stream.
   */
  def init(exampleSpecification: ExampleSpecification): Unit

  /** 
   * Train the model based on the algorithm implemented in the learner, 
   * from the stream of Examples given for training.
   * 
   * @param input a stream of Examples
   */
  def train(input: DStream[Example]): Unit

  /**
   * Gets the current Model used for the Learner.
   * 
   * @return the Model object used for training
   */
  def getModel: T
}
