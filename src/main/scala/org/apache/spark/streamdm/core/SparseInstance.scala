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
 * A SparseInstance is an Instance in which the features are sparse, i.e., most
 * features will not have any value.
 * The SparseInstance will keep two Arrays: one with the values and one with the
 * corresponding indexes.
 */

case class SparseSingleLabelInstance(inIndexes:Array[Int],
                                     inValues:Array[Double],
                                     inLabel: Double)
  extends Instance with Serializable {
  
  type T = SparseSingleLabelInstance

  val indexes = inIndexes
  val values = inValues
  override val label = inLabel

  /* Get the feature value present at position index
  *
  * @param index the index of the features
  * @return a Double representing the feature value
  */
  def featureAt(index: Int): Double = {
    var i: Int = 0
    var value: Double = 0.0
    var found = false
    while(i<indexes.length && !found) {
      if(indexes(i)==index) {
        value = values(i)
        found = true
      }
      i += 1
    }
    value
  }
  
  /*Get the class value present at position index
  *
  * @param index the index of the class
  * @return a Double representing the value fo the class
  */
  def labelAt(index: Int): Double = label

  /* Perform a dot product between two instances
  *
  * @param input an Instance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = {
    var i: Int = 0
    var dot: Double = 0.0
    while(i<indexes.length) {
      dot += values(i)*input.featureAt(indexes(i))
      i += 1
    }
    dot
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return a SparseInstance representing the added Instances
   */
  override def add(input: Instance): SparseSingleLabelInstance = input match {
    case SparseSingleLabelInstance(ind,v,l) => {
      var i: Int = 0
      var addedFeatures: Array[Double] = Array()
      var addedIndexes: Array[Int] = Array()
      while(i<ind.length) {
        val sum = v(i) + featureAt(ind(i))
        if(v(i)!=0 && sum!=0) {
          addedIndexes :+= ind(i)
          addedFeatures :+= sum
        }
        i += 1
      }
      i = 0
      while(i<indexes.length) {
        val other = input.featureAt(indexes(i))
        if(other==0 && values(i)!=0) {
          addedIndexes :+= indexes(i)
          addedFeatures :+= values(i)
        }
        i += 1
      }
      new SparseSingleLabelInstance(addedIndexes, addedFeatures, label)
    }
    case DenseSingleLabelInstance(f,l) => {
      var i: Int = 0
      var addedFeatures: Array[Double] = Array()
      var addedIndexes: Array[Int] = Array()
      while(i<f.length) {
        val sum = f(i) + featureAt(i)
        if(sum!=0) {
          addedIndexes :+= i
          addedFeatures :+= sum
        }
        i += 1
      }
      new SparseSingleLabelInstance(addedIndexes, addedFeatures, label)
    }
    case _ => this
  }

  /** Append a feature to the instance
   *
   * @param input the value which is added up
   * @return a SparseInstance representing the new feature vector
   */
  override def setFeature(index: Int, input: Double):SparseSingleLabelInstance =
    new SparseSingleLabelInstance(indexes:+index,values:+input,label)

  /** Apply an operation to every feature of the Instance (essentially a map)
   * @param func the function for the transformation
   * @return a new SparseInstance with the transformed features
   */
  override def mapFeatures(func: Double=>Double): SparseSingleLabelInstance =
    new SparseSingleLabelInstance(indexes, values.map{case x => func(x)}, label)

  override def toString =
      "l=%.0f i={%s} v={%s}".format(label, indexes.mkString(","),
        values.mkString(","))
  
}

object SparseSingleLabelInstance extends Serializable {
  
  /** Parse the input string as an SparseInstance class
   *
   * @param input the String line to be read, in LibSVM format
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): SparseSingleLabelInstance = {
    val tokens = input.split("\\s+")
    val features = tokens.tail.map(_.split(":"))
    new SparseSingleLabelInstance(features.map(_(0).toInt-1),
                                  features.map(_(1).toDouble),
                                  tokens.head.toDouble)
  }
}
