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

case class TextInstance(inFeatures: Map[String, Double])
    extends Instance with Serializable {

  type T = TextInstance

  val features = inFeatures

  /* Get the feature value for a given index 
  *
  * @param index the key of the features
  * @return a Double representing the feature value
  */
  def apply(index: Int): Double =
    valueAt(index.toString)

  /*
   * Return an array of features and indexes
   *
   * @return an array of turple2(value,index)
   */
  def getFeatureIndexArray(): Array[(Double, Int)] = features.toArray.map{x =>(x._2,x._1.toInt)}

  /* Get the feature value for a given key
  *
  * @param key the key of the features
  * @return a Double representing the feature value
  */
  def valueAt(key: String): Double =
    features.getOrElse(key, 0.0)

  /* Perform a dot product between two instances
  *
  * @param input an Instance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = input match {
    case TextInstance(f) =>
      dotTupleArrays(f.toArray, features.toArray)
    case _ => 0.0
  }

  /**
   * Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): TextInstance = input match {
    case TextInstance(f) => {
      val addedInstance = addTupleArrays(f.toArray, features.toArray)
      new TextInstance(arrayToMap(addedInstance))
    }
    case _ => this
  }

  /**
   * Perform an element by element multiplication between two instances
   *
   * @param input an Instance which is multiplied
   * @return an Instance representing the Hadamard product
   */
  override def hadamard(input: Instance): TextInstance = input match {
    case TextInstance(f) => {
      val addedInstance = mulTupleArrays(f.toArray, features.toArray)
      new TextInstance(arrayToMap(addedInstance))
    }
    case _ => this

  }

  /**
   * Compute the Euclidean distance to another Instance
   *
   * @param input the Instance to which the distance is computed
   * @return a Double representing the distance value
   */
  override def distanceTo(input: Instance): Double = input match {
    case TextInstance(f) => {
      var sum: Double = 0.0
      for ((k, v) <- f)
        if (v != 0) sum += math.pow(valueAt(k) - v, 2.0)
      for ((k, v) <- features)
        if (f.getOrElse(k, 0.0) == 0) sum += math.pow(v, 2.0)
      math.sqrt(sum)
    }
    case _ => Double.MaxValue
  }

  /**
   * Append a feature to the instance
   *
   * @param key the key on which the feature is set
   * @param value the value on which the feature is set
   * @return an Instance representing the new feature vector
   */
  def setFeature(key: String, value: Double): TextInstance =
    new TextInstance((features - key) + (key -> value))

  /**
   * Append a feature to the instance
   *
   * @param index the index at which the feature is set
   * @param input the new value of the feature
   * @return an Instance representing the new feature vector
   */
  def set(index: Int, input: Double): TextInstance =
    setFeature(index.toString, input)

  /**
   * Apply an operation to every feature of the Instance (essentially a map)
   * TODO try to extend map to this case
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def map(func: Double => Double): TextInstance =
    new TextInstance(features.mapValues { case x => func(x) })

  /**
   * Aggregate the values of an instance
   *
   * @param func the function for the transformation
   * @return the reduced value
   */
  override def reduce(func: (Double, Double) => Double): Double =
    features.valuesIterator.reduce(func)

  private def dotTupleArrays(l1: Array[(String, Double)],
    l2: Array[(String, Double)]): Double =
    (l1 ++ l2).groupBy(_._1).filter { case (k, v) => v.length == 2 }.
      map { case (k, v) => (k, v.map(_._2).reduce(_ * _)) }.toArray.unzip._2.sum

  private def addTupleArrays(l1: Array[(String, Double)],
    l2: Array[(String, Double)]): Array[(String, Double)] =
    (l1 ++ l2).groupBy(_._1).map { case (k, v) => (k, v.map(_._2).sum) }.
      toArray.filter(_._2 != 0)

  private def mulTupleArrays(l1: Array[(String, Double)],
    l2: Array[(String, Double)]): Array[(String, Double)] =
    (l1 ++ l2).groupBy(_._1).map { case (k, v) => (k, v.map(_._2).product) }.
      toArray.filter(_._2 != 0)

  private def arrayToMap(l: Array[(String, Double)]): Map[String, Double] =
    l.groupBy(_._1).map { case (k, v) => (k, v.head._2) }

}

object TextInstance extends Serializable {

  /**
   * Parse the input string as an SparseInstance class
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): TextInstance = {
    val tokens = input.split(",")
    val features = tokens.map(_.split(":"))
    val featMap = features.groupBy(_.head).map {
      case (k, v) => (k,
        v.head.tail.head.toDouble)
    }
    new TextInstance(featMap)
  }
}
