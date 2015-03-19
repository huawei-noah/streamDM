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

case class DenseSingleLabelInstance(inFeatures: Array[Double], inLabel: Double)
  extends Instance with Serializable {
  
  type T = DenseSingleLabelInstance

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
  override def dot(input: Instance): Double = input match { 
    case DenseSingleLabelInstance(f,l) => {
      var sum:Double = 0.0
      var i:Int = 0
      while (i<features.length) {
        sum += f(i)*featureAt(i)
        i += 1
      }
      sum
    }
      //normally it should be implemented as below
      //(0 until features.length).foldLeft(0.0)((d,i) => d + features(i)*f(i))
    case SparseSingleLabelInstance(i,v,l) =>
      input.dot(this)
    case _ => 0.0
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): DenseSingleLabelInstance = input match {
    case DenseSingleLabelInstance(f,l) =>
      new DenseSingleLabelInstance((features zip f).
        map{case (x,y) => x+y}, label)
    case SparseSingleLabelInstance(i,v,l) => {
      //the below uses a mutable Array, but I can't see any other way that
      //only uses one pass over the structures in SparseInstance
      var arrayFromSparse = Array.fill[Double](features.length)(0.0)
      (i zip v).foreach{ case (x,y) => arrayFromSparse.update(x,y)}
      new DenseSingleLabelInstance((features zip arrayFromSparse).
        map{case (x,y) => x+y}, label)
    }
    case _ => this
  }

  /** Add a feature to the instance
   *
   * @param index the index at which the value is added
   * @param input the feature value which is added up
   * @return an Instance representing the new feature vector
   */
  override def setFeature(index: Int, input: Double):DenseSingleLabelInstance =
    new DenseSingleLabelInstance(features:+input,label)

  /** Apply an operation to every feature of the Instance (essentially a map)
   * TODO try to extend map to this case
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def mapFeatures(func: Double=>Double): DenseSingleLabelInstance =
    new DenseSingleLabelInstance(features.map{case x => func(x)}, label)
 
  override def toString =
      "l=%.0f v={%s}".format(label, features.mkString(","))
}

object DenseSingleLabelInstance extends Serializable {
  
  /** Parse the input string as an DenseInstance class
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): DenseSingleLabelInstance = {
    val tokens = input.split("\\s+")
    new DenseSingleLabelInstance(tokens.tail.map(_.toDouble),
                                 tokens.head.toDouble)
  }
}
