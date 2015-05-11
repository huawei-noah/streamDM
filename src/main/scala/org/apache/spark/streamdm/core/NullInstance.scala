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
 * A NullInstance is an Instance which does not contain anything in it. It
 * specified so that we keep the Example design when  
 */

case class NullInstance extends Instance with Serializable {
  
  type T = NullInstance

  /* Get the feature value present at position index
  *
  * @param index the index of the desired value 
  * @return a Double representing the feature value
  */
  override def apply(index: Int): Double = 0.0 

  /* Perform a dot product between two instances
  *
  * @param input an Instance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = 0.0

  /** Compute the Euclidean distance to another Instance 
   *
   * @param input the Instance to which the distance is computed
   * @return a Double representing the distance value
   */
  def distanceTo(input: Instance): Double = Double.MaxValue

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): NullInstance = this

  /** Perform an element by element multiplication between two instances
   *
   * @param input an Instance which is multiplied
   * @return an Instance representing the Hadamard product
   */
  override def hadamard(input: Instance): NullInstance = this

  /** Add a feature to the instance
   *
   * @param index the index at which the value is added
   * @param input the feature value which is added up
   * @return an Instance representing the new feature vector
   */
  override def set(index: Int, input: Double): NullInstance = this

  /** Apply an operation to every feature of the Instance (essentially a map)
   * TODO try to extend map to this case
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def map(func: Double=>Double): NullInstance = this

  /** Aggregate the values of an instance 
   *
   * @param func the function for the transformation
   * @return the reduced value
   */
  override def reduce(func: (Double,Double)=>Double): Double = 0.0

  override def toString = "" 
}
