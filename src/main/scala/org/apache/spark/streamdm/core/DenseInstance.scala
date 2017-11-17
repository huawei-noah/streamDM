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
 * corresponding operations will be based on this data structure.
 */
case class DenseInstance(inVector: Array[Double])
    extends Instance with Serializable {

  type T = DenseInstance

  val features = inVector

  /* Get the feature value present at position index
  * In case the index does not exist or is invalid, then NaN is returned.
  *
  * @param index the index of the desired value 
  * @return a Double representing the feature value
  */
  override def apply(index: Int): Double =
    if (index >= features.length || index < 0) Double.NaN else features(index)

  /*
   * Return an array of features and indexes
   *
   * @return an array of turple2(value,index)
   */
  def getFeatureIndexArray(): Array[(Double, Int)] = features.zipWithIndex

  /* Perform a dot product between two instances
  *
  * @param input an DenseSingleLabelInstance with which the dot
  * product is performed
  * @return a Double representing the dot product 
  */
  override def dot(input: Instance): Double = input match {
    case DenseInstance(f) => {
      var sum: Double = 0.0
      var i: Int = 0
      while (i < features.length) {
        sum += f(i) * this(i)
        i += 1
      }
      sum
    }
    //using imperative version for efficiency reasons
    //normally it should be implemented as below
    //  (0 until features.length).foldLeft(0.0)((d,i) => d + features(i)*f(i))
    case SparseInstance(i, v) =>
      input.dot(this)
    case _ => 0.0
  }

  /**
   * Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  override def add(input: Instance): DenseInstance = input match {
    case DenseInstance(f) => {
      var newF: Array[Double] = Array.fill(features.length)(0.0)
      var i: Int = 0
      while (i < features.length) {
        newF(i) = features(i) + f(i)
        i += 1
      }
      new DenseInstance(newF)
    }
    //val addedInstance = (0 until features.length).map(i => features(i)+f(i))
    //new DenseSingleLabelInstance(addedInstance.toArray, label)
    case SparseInstance(ind, v) => {
      var newF: Array[Double] = Array.fill(features.length)(0.0)
      var i: Int = 0
      while (i < features.length) {
        newF(i) = features(i)
        i += 1
      }
      i = 0
      while (i < ind.length) {
        newF(ind(i)) += v(i)
        i += 1
      }
      new DenseInstance(newF)
    }
    case _ => new DenseInstance(features)
  }

  /**
   * Perform an element by element multiplication between two instances
   *
   * @param input an Instance which is multiplied
   * @return an Instance representing the Hadamard product
   */
  override def hadamard(input: Instance): DenseInstance = input match {
    case DenseInstance(f) => {
      var newF: Array[Double] = Array.fill(features.length)(0.0)
      var i: Int = 0
      while (i < features.length) {
        newF(i) = features(i) * f(i)
        i += 1
      }
      new DenseInstance(newF)
    }
    case SparseInstance(ind, v) => {
      var newF: Array[Double] = Array.fill(features.length)(0.0)
      var i: Int = 0
      while (i < ind.length) {
        newF(ind(i)) = features(ind(i)) * v(i)
        i += 1
      }
      new DenseInstance(newF)
    }
    case _ => new DenseInstance(features)
  }

  /**
   * Compute the Euclidean distance to another Instance
   *
   * @param input the Instance to which the distance is computed
   * @return a Double representing the distance value
   */
  override def distanceTo(input: Instance): Double = input match {
    case DenseInstance(f) => {
      var sum: Double = 0.0
      var i: Int = 0
      while (i < features.length) {
        sum += math.pow(features(i) - f(i), 2.0)
        i += 1
      }
      math.sqrt(sum)
    }
    case SparseInstance(ind, v) => {
      var sum: Double = 0.0
      var i: Int = 0
      while (i < ind.length) {
        if (v(i) != 0) sum += math.pow(features(ind(i)) - v(i), 2.0)
        i += 1
      }
      i = 0
      while (i < features.length) {
        if (input(i) == 0) sum += math.pow(features(i), 2.0)
        i += 1
      }
      math.sqrt(sum)
    }
    case _ => Double.MaxValue
  }

  /**
   * Add a feature to the instance
   *
   * @param index the index at which the value is added
   * @param input the feature value which is added up
   * @return an Instance representing the new feature vector
   */
  override def set(index: Int, input: Double): DenseInstance = {
    var returnInstance = this
    if (index >= 0 && index < features.length) {
      features(index) = input
      returnInstance = new DenseInstance(features)
    } else if (index == features.length)
      returnInstance = new DenseInstance(features :+ input)
    returnInstance
  }

  /**
   * Apply an operation to every feature of the Instance
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  override def map(func: Double => Double): DenseInstance =
    new DenseInstance(features.map { case x => func(x) })

  /**
   * Aggregate the values of an instance
   *
   * @param func the function for the transformation
   * @return the aggregated value
   */
  override def reduce(func: (Double, Double) => Double): Double =
    features.reduce(func)

  override def toString = features.mkString(",")
}

object DenseInstance extends Serializable {

  /**
   * Parse the input string as an DenseInstance class, where each features is
   * separated by a comma (CSV format).
   *
   * @param input the String line to be read
   * @return a DenseInstance which is parsed from input
   */
  def parse(input: String): DenseInstance = {
    val tokens = input.split(",")
    new DenseInstance(tokens.map(_.toDouble))
  }
}
