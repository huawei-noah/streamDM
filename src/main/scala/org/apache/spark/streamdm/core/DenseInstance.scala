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
 * The DenseInstance will keep an Array of the values and the
 * corresponding dot product will be based on that.
 */

case class DenseInstance(inVector: Array[Double])
  extends Instance with Serializable {
  
  type T = DenseInstance

  val features = inVector

  /* Get the feature value present at position index
  *
  * @param index the index of the desired value 
  * @return a Double representing the feature value
  */
  override def apply(index: Int): Double = 
    if (index>=features.length||index<0) 0.0 else features(index)

  /* Perform a dot product between two instances
  *
  * @param input an DenseSingleLabelInstance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = input match { 
    case DenseInstance(f) => {
      var sum:Double = 0.0
      var i:Int = 0
      while (i<features.length) {
        sum += f(i)*this(i)
        i += 1
      }
      sum
    }
    //using imperative version for efficiency reasons
    //normally it should be implemented as below
    //  (0 until features.length).foldLeft(0.0)((d,i) => d + features(i)*f(i))
    case SparseInstance(i,v) =>
      input.dot(this)
    case _ => 0.0
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): DenseInstance = input match {
    case DenseInstance(f) => {
      var i: Int = 0
      while (i<features.length) {
        features(i) += f(i)
        i += 1
      }
      new DenseInstance(features)
    }
      //val addedInstance = (0 until features.length).map(i => features(i)+f(i))
      //new DenseSingleLabelInstance(addedInstance.toArray, label)
    case SparseInstance(ind,v) => {
      var i: Int = 0
      while (i<ind.length) {
        features(ind(i)) += v(i)
        i += 1
      }
      new DenseInstance(features)
    }
    case _ => this
  }

  /** Add a feature to the instance
   *
   * @param index the index at which the value is added
   * @param input the feature value which is added up
   * @return an Instance representing the new feature vector
   */
  override def set(index: Int, input: Double): DenseInstance = {
    var returnInstance = this
    if (index>=0&&index<features.length) {
      features(index) = input
      returnInstance = new DenseInstance(features)
    }
    else if (index==features.length)
      returnInstance = new DenseInstance(features:+input)
    returnInstance
  }

  /** Apply an operation to every feature of the Instance (essentially a map)
   * TODO try to extend map to this case
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def map(func: Double=>Double): DenseInstance =
    new DenseInstance(features.map{case x => func(x)})
 
  override def toString = features.mkString(",")
}

object DenseInstance extends Serializable {
  
  /** Parse the input string as an DenseInstance class
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): DenseInstance = {
    val tokens = input.split(",")
    new DenseInstance(tokens.map(_.toDouble))
  }
}
