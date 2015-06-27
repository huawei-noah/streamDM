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

import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * A Classifier trait defines the needed operations on any implemented
 * classifier. It provides methods for training the model and for predicting the
 * labels for a stream of Instance RDDs.
 */
trait Classifier extends Learner with Serializable {

  /** Builds a stream of Examples and predictions based on the algorithm implemented in the classifier,
   * from the stream of instances given for testing.
   *
   * @param input a stream of examples
   * @return a stream of examples and numeric values
   */
  def predict(input: DStream[Example]): DStream[(Example, Double)]
}
