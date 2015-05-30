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

import org.apache.spark.streamdm.core.{ Example }

/**
 * trait ConditionalTestType and the case classes
 */
trait ConditionalTestType

case class NumericBinaryTestType() extends ConditionalTestType
case class NominalBinaryTestType() extends ConditionalTestType
case class NominalMultiwayTestType() extends ConditionalTestType
case class NumericBinaryRulePredicateType() extends ConditionalTestType

/**
 * ConditionalTest is a condition test trait for examples
 * to use to split nodes in Hoeffding trees.
 */
trait ConditionalTest extends Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the Exmaple to be used
   * @return the number of the branch for an example, -1 if unknown.
   */
  def branch(example: Example): Int

  /**
   * Gets whether the number of the branch for an example is known.
   *
   * @param example Example
   * @return true if the number of the branch for an example is known
   */
  def hasResult(example: Example): Boolean = { branch(example) >= 0 }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown..
   */
  def maxBranches(): Int

}

/**
 * Numeric binary conditional test for examples to use to split nodes in Hoeffding trees.
 */

case class NumericBinaryTest(val fIndex: Int, val value: Double, val isequalTest: Boolean)
  extends ConditionalTest with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the Exmaple to be used
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
}
/**
 * Nominal binary conditional test for examples to use to split nodes in Hoeffding trees.
 */
case class NominalBinaryTest(val fIndex: Int, val value: Double)
  extends ConditionalTest with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the Exmaple to be used
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    if (example.featureAt(fIndex) < value) 0 else 1
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown..
   */
  override def maxBranches(): Int = 2

  override def toString(): String = {
    "NominalBinaryTest feature[" + fIndex + "] = " + value
  }
}

/**
 * Nominal multi-way conditional test for examples to use to split nodes in Hoeffding trees.
 */
case class NominalMultiwayTest(val fIndex: Int) extends ConditionalTest with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the Exmaple to be used
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    example.featureAt(fIndex).toInt
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown..
   */
  override def maxBranches(): Int = -1

  override def toString(): String = "NominalMultiwayTest"
}

/**
 * Numeric binary rule predicate conditional test for
 *  examples to use to split nodes in Hoeffding trees.
 */
case class NumericBinaryRulePredicate(val fIndex: Int, val value: Double, val operator: Int)
  extends ConditionalTest with Serializable {

  /**
   *  Returns the number of the branch for an example, -1 if unknown.
   *
   * @param example the Exmaple to be used
   * @return the number of the branch for an example, -1 if unknown.
   */
  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    operator match {
      // operator: 0 ==, 1 <=, 2 >
      case 0 => if (v == value) 0 else 1
      case 1 => if (v <= value) 0 else 1
      case 2 => if (v > value) 0 else 1
      case _ => if (v == value) 0 else 1
    }
  }

  /**
   * Gets the number of maximum branches, -1 if unknown.
   *
   * @return the number of maximum branches, -1 if unknown..
   */
  override def maxBranches(): Int = 2
}