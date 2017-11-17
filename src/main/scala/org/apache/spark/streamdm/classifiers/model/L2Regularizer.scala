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
 * The L2Regularizer gradient returns the weight as the gradient for the
 * regularization.
 */
class L2Regularizer extends Regularizer with Serializable {
  /** Computes the value of the gradient function
   * @param valweightue the weight for which the gradient is computed
   * @return the gradient value 
   */
  def gradient(weight: Double): Double = weight 
}
