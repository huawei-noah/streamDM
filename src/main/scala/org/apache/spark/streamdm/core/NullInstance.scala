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
 * A NullInstance is an Instance which does not contain anything in it. It is
 * present to aid the design of Example, and to allow cases when we have
 * instances which do not have output values.
 * Every operation on a NullInstance with either return a NullInstance or a
 * value of 0.
 */

case class NullInstance() extends Instance with Serializable {

  type T = NullInstance

  /* Get the feature value present at position index
  *
  * @param index the index of the desired value 
  * @return a value of 0 
  */
  override def apply(index: Int): Double = 0.0

  /*
   * Return an array of features and indexes
   *
   * @return an array of turple2(value,index)
   */
  def getFeatureIndexArray(): Array[(Double, Int)] = new Array[(Double, Int)](0)

  /* Perform a dot product between two instances
  *
  * @param input an Instance with which the dot
  * product is performed
  * @return a value of 0 
  */
  override def dot(input: Instance): Double = 0.0

  /**
   * Compute the Euclidean distance to another Instance
   *
   * @param input the Instance to which the distance is computed
   * @return an infinite distance (implemented as Double.MaxValue)
   */
  def distanceTo(input: Instance): Double = Double.MaxValue

  /**
   * Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return a NullInstance
   */
  override def add(input: Instance): NullInstance = this

  /**
   * Perform an element by element multiplication between two instances
   *
   * @param input an Instance which is multiplied
   * @return a NullInstance
   */
  override def hadamard(input: Instance): NullInstance = this

  /**
   * Add a feature to the instance
   *
   * @param index the index at which the value is added
   * @param input the feature value which is added up
   * @return a NullInstance
   */
  override def set(index: Int, input: Double): NullInstance = this

  /**
   * Apply an operation to every feature of a NullInstance
   * @param func the function for the transformation
   * @return a NullInstance
   */
  override def map(func: Double => Double): NullInstance = this

  /**
   * Aggregate the values of a NullInstance
   *
   * @param func the function for the transformation
   * @return a value of 0
   */
  override def reduce(func: (Double, Double) => Double): Double = 0.0

  override def toString = ""
}
