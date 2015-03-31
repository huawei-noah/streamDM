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

import com.github.javacliparser.Configurable
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._
/**
 * trait IncrementalLearner used for incremental learning.
 *
 */
trait IncrementalLearner extends Learner with Configurable with Serializable {
  
    /* run for evaluation, first predict the label of the Instance, then update the model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  def evaluate(input: DStream[Example]): DStream[(Example, Double)]
}