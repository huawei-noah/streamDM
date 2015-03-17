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
 * A TextInstance is an Instance in which the features are a map of text keys,
 * each associated with a value.
 */

case class TextSingleLabelInstance(inFeatures: Map[String, Double],
                                   inLabel: Double)
  extends Instance with Serializable {
  
  type T = TextSingleLabelInstance

  val features = inFeatures
  override val label = inLabel

 /* Get the feature value for a given index 
  *
  * @param index the key of the features
  * @return a Double representing the feature value
  */
  def featureAt(index: Int): Double =
    featureAt(index.toString)

  /* Get the feature value for a given key
  *
  * @param key the key of the features
  * @return a Double representing the feature value
  */
  def featureAt(key: String): Double =
    features.getOrElse(key,0.0)
  
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
    case TextSingleLabelInstance(f,l) =>
      dotTupleArrays(f.toArray, features.toArray)
    case _ => 0.0
  }

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): TextSingleLabelInstance = input match {
    case TextSingleLabelInstance(f,l) => {
      val addedInstance = addTupleArrays(f.toArray,features.toArray)
      new TextSingleLabelInstance(arrayToMap(addedInstance), label)
    }
    case _ => this
  }

  /** Append a feature to the instance
   *
   * @param key the key on which the feature is set
   * @param value the value on which the feature is set
   * @return an Instance representing the new feature vector
   */
  def setFeature(key: String, value: Double): TextSingleLabelInstance =
    new TextSingleLabelInstance((features-key)+(key->value), label)

  /** Append a feature to the instance
   *
   * @param index the index at which the feature is set
   * @param input the new value of the feature
   * @return an Instance representing the new feature vector
   */
  def setFeature(index: Int, input: Double): TextSingleLabelInstance =
    setFeature(index.toString, input)

  /** Apply an operation to every feature of the Instance (essentially a map)
   * TODO try to extend map to this case
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def mapFeatures(func: Double=>Double): TextSingleLabelInstance =
    new TextSingleLabelInstance(features.mapValues{case x => func(x)}, label) 
  
  private def dotTupleArrays(l1: Array[(String, Double)], 
                             l2: Array[(String, Double)]): Double =
    (l1++l2).groupBy(_._1).filter {case (k,v) => v.length==2}.
        map {case (k,v) => (k, v.map(_._2).reduce(_*_))}.toArray.unzip._2.sum

  private def addTupleArrays(l1: Array[(String, Double)], 
                          l2: Array[(String, Double)]):Array[(String,Double)] =
    (l1++l2).groupBy(_._1).map {case (k,v) => (k, v.map(_._2).sum)}.
      toArray.filter(_._2 != 0)

  private def arrayToMap(l: Array[(String,Double)]): Map[String, Double] =
    l.groupBy(_._1).map{case (k,v) => (k,v.head._2)}
  
}

object TextSingleLabelInstance extends Serializable {
  
  /** Parse the input string as an SparseInstance class
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): TextSingleLabelInstance = {
    val tokens = input.split("\\s+")
    val features = tokens.tail.map(_.split(":"))
    val featMap = features.groupBy(_.head).map{case (k,v) => (k,
      v.head.tail.head.toDouble)}
    new TextSingleLabelInstance(featMap, tokens.head.toDouble)
  }
}
