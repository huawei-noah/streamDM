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

package org.apache.spark.streamdm.classifiers.model

/**
 * Implementation of the perceptron loss function. Essentially, the perceptron
 * is using the squared loss function except for the "gradient".
 */

class PerceptronLoss extends SquaredLoss with Serializable {
  /** Computes the value of the perceptron update function
   * @param label the label against which the update is computed
   * @param dot the dot product of the linear model and the instance
   * @return the update value 
   */
  override def gradient(label: Double, dot: Double): Double =
    label-predict(dot)

}
