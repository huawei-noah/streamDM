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
    val tuple = (indexes zip values).filter(_._1==index)
    if (tuple.length>0) tuple(0)._2 else 0.0
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
  override def dot(input: Instance): Double = input match { 
    case SparseSingleLabelInstance(i,v,l) =>
      dotTupleArrays(i zip v, indexes zip values)
    case DenseSingleLabelInstance(f,l) =>
      dotTupleArrays(f.zipWithIndex.map{case (x,y)=>(y,x)},
                     indexes zip values)
    case _ => 0.0
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return a SparseInstance representing the added Instances
   */
  override def add(input: Instance): SparseSingleLabelInstance = input match {
    case SparseSingleLabelInstance(i,v,l) => {
      val addedInstance = addTupleArrays(i zip v, indexes zip values).unzip
      new SparseSingleLabelInstance(addedInstance._1.toArray,
                                    addedInstance._2.toArray, label)
    }
    case DenseSingleLabelInstance(f,l) => {
      val addedInstance = addTupleArrays(f.zipWithIndex.map{case (x,y)=>(y,x)},
                                        indexes zip values).unzip
      new SparseSingleLabelInstance(addedInstance._1.toArray,
                                    addedInstance._2.toArray, label)
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
  private def dotTupleArrays(l1: Array[(Int, Double)], 
                             l2: Array[(Int, Double)]): Double =
    (l1++l2).groupBy(_._1).filter {case (k,v) => v.length==2}.
        map {case (k,v) => (k, v.map(_._2).reduce(_*_))}.toArray.unzip._2.sum

  private def addTupleArrays(l1: Array[(Int, Double)], 
                            l2: Array[(Int, Double)]): Array[(Int, Double)] =
    (l1++l2).groupBy(_._1).map {case (k,v) => (k, v.map(_._2).sum)}.
      toArray.filter(_._2 != 0)
  
}

object SparseSingleLabelInstance extends Serializable {
  
  /** Parse the input string as an SparseInstance class
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): SparseSingleLabelInstance = {
    val tokens = input.split("\t")
    val features = tokens.tail.map(_.split(":"))
    new SparseSingleLabelInstance(features.map(_(0).toInt),
                                  features.map(_(1).toDouble),
                                  tokens.head.toDouble)
  }
}
