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
 * A DenseInstance is an Instance in which the features are dense, i.e., there
 * exists a value for (almost) every feature.
 * The DenseInstance will keep an Array of the values of the features, and the
 * corresponding dot product will be based on that.
 */

class DenseSingleLabelInstance(inFeatures: Array[Double], inLabel: Double)
  extends Instance[DenseSingleLabelInstance] with Serializable {
  
  val features = inFeatures
  override val label = inLabel

  /* Get the feature value present at position index
  *
  * @param index the index of the features
  * @return a Double representing the feature value
  */
  def featureAt(index: Int): Double = features(index)
  
  /*Get the class value present at position index
  *
  * @param index the index of the class
  * @return a Double representing the value fo the class
  */
  def labelAt(index: Int): Double = label

  /* Perform a dot product between two instances
  *
  * @param input an DenseSingleLabelInstance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: DenseSingleLabelInstance): Double = 
    ((features zip input.features).map{case (x,y)=>x*y}).reduce(_+_)

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: DenseSingleLabelInstance): DenseSingleLabelInstance =
    new DenseSingleLabelInstance((features zip input.features).
      map{case (x,y) => x+y}, label)

  /** Append a feature to the instance
   *
   * @param input the value which is added up
   * @return an Instance representing the new feature vector
   */
  override def append(input: Double): DenseSingleLabelInstance =
    new DenseSingleLabelInstance(features:+1.0,label)

  /** Apply an operation to every feature of the Instance (essentially a map)
   *
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def mapFeatures(func: Double=>Double): DenseSingleLabelInstance =
    new DenseSingleLabelInstance(features.map{case x => func(x)}, label)
  
}
