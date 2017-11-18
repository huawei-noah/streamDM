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

package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.core.Example

/**
 * ConditionalTest is an abstract class for  conditional tests, used for
 * splitting nodes in Hoeffding trees.
 */
abstract class ConditionalTest(var fIndex: Int) extends Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the input Example 
   * @return the number of the branch for an example, -1 if unknown.
   */
  def branch(example: Example): Int

  /**
   * Gets whether the number of the branch for an example is known.
   *
   * @param example the input Example
   * @return true if the number of the branch for an example is known
   */
  def hasResult(example: Example): Boolean = { branch(example) >= 0 }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown.
   */
  def maxBranches(): Int

  /**
   * Returns the index of the feature
   *
   * @return the index of the feature
   */
  def featureIndex(): Int = fIndex

  /**
   * Get the conditional test description.
   *
   * @return an Array containing the description
   */
  def description(): Array[String]

}

/**
 * Numeric binary conditional test for splitting nodes in Hoeffding trees.
 */

class NumericBinaryTest(fIndex: Int, val value: Double, val isequalTest: Boolean)
  extends ConditionalTest(fIndex) with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the input Example. 
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    if (isequalTest) {
      if (v == value) 0 else 1
    } else {
      if (v < value) 0 else 1
    }
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown..
   */
  override def maxBranches(): Int = 2

  /**
   * Returns the index of the tested feature
   *
   * @return the index of the tested feature
   */
  override def featureIndex(): Int = fIndex

  /**
   * Get the conditional test description.
   *
   * @return an Array containing the description
   */
  override def description(): Array[String] = {
    val des = new Array[String](2)
    val ops = if (isequalTest) Array("==", "!=") else Array("<", ">=")
    des(0) = "[feature " + fIndex + " numeric 0] " + ops(0) + " " + value
    des(1) = "[feature " + fIndex + " numeric 1] " + ops(1) + " " + value
    des
  }

  override def toString = "NumericBinaryTest(" + isequalTest + ") feature[" + fIndex + "] = " +
                           value
}
/**
 * Nominal binary conditional test for splitting nodes in Hoeffding trees.
 */
class NominalBinaryTest(fIndex: Int, val value: Double)
  extends ConditionalTest(fIndex) with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the input example
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    if (example.featureAt(fIndex) == value) 0 else 1
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown.
   */
  override def maxBranches(): Int = 2

  override def toString(): String = {
    "NominalBinaryTest feature[" + fIndex + "] = " + value

  }

  /**
   * Get the conditional test description.
   *
   * @return an Array containing the description
   */
  override def description(): Array[String] = {
    val des = new Array[String](2)
    des(0) = "[feature " + fIndex + " nominal 0] == " + value
    des(1) = "[feature " + fIndex + " nominal 1] != " + value
    des
  }
}

/**
 * Nominal multi-way conditional test for splitting nodes in Hoeffding trees.
 */
class NominalMultiwayTest(fIndex: Int, val numFeatureValues: Int)
  extends ConditionalTest(fIndex) with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the input example 
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    example.featureAt(fIndex).toInt
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown.
   */
  override def maxBranches(): Int = numFeatureValues

  override def toString(): String = "NominalMultiwayTest" + "feature[" + fIndex + "] " +
                                      numFeatureValues

  /**
   * Get the conditional test description.
   *
   * @return an Array containing the description
   */
  override def description(): Array[String] = {
    val des = new Array[String](numFeatureValues)
    for (i <- 0 until numFeatureValues)
      des(i) = "[feature " + fIndex + " nominal " + i + "] == " + i
    des
  }
}

/**
 * Numeric binary rule predicate test for splitting nodes in Hoeffding trees.
 */
class NumericBinaryRulePredicate(fIndex: Int, val value: Double, val operator: Int)
  extends ConditionalTest(fIndex) with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the input example 
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    operator match {
      // operator: 0 ==, 1 <=, 2 < different from MOA which is > 
      case 0 => if (v == value) 0 else 1
      case 1 => if (v <= value) 0 else 1
      case 2 => if (v < value) 0 else 1
      case _ => if (v == value) 0 else 1
    }
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown.
   */
  override def maxBranches(): Int = 2

  /**
   * Get the conditional test description.
   *
   * @return an Array containing the description
   */
  override def description(): Array[String] = {
    val des = new Array[String](2)
    val ops = if (operator == 0) Array("==", "!=") else if (operator == 1) 
                Array("<=", ">") else Array("<", ">=")
    des(0) = "[feature " + fIndex + " numeric 0] " + ops(0) + " " + value
    des(1) = "[feature " + fIndex + " numeric 1] " + ops(1) + " " + value
    des
  }
}
