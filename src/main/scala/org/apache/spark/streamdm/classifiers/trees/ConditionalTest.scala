package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.core.{ Example }

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

  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    if (isequalTest) {
      if (v == value) 0 else 1
    } else {
      if (v < value) 0 else 1
    }
  }

  override def maxBranches(): Int = 2
}
/**
 * Nominal binary conditional test for examples to use to split nodes in Hoeffding trees.
 */
case class NominalBinaryTest(val fIndex: Int, val value: Double)
  extends ConditionalTest with Serializable {

  override def branch(example: Example): Int = {
    // todo process missing value
    if (example.featureAt(fIndex) < value) 0 else 1
  }

  override def maxBranches(): Int = 2
}
/**
 * Nominal multi-way conditional test for examples to use to split nodes in Hoeffding trees.
 */
case class NominalMultiwayTest(val fIndex: Int) extends ConditionalTest with Serializable {

  override def branch(example: Example): Int = {
    // todo process missing value
    example.featureAt(fIndex).toInt
  }

  override def maxBranches(): Int = -1
}

/**
 * Numeric binary rule predicate conditional test for
 *  examples to use to split nodes in Hoeffding trees.
 */
case class NumericBinaryRulePredicate(val fIndex: Int, val value: Double, val operator: Int)
  extends ConditionalTest with Serializable {
  // operator: 0 ==, 1 <=, 2 >
  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    operator match {
      case 0 => if (v == value) 0 else 1
      case 1 => if (v <= value) 0 else 1
      case 2 => if (v > value) 0 else 1
      case _ => if (v == value) 0 else 1
    }
  }

  override def maxBranches(): Int = 2
}