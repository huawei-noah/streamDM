package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.core.{ Example }

trait ConditionalTestType

case class NumericBinaryTestType() extends ConditionalTestType
case class NominalBinaryTestType() extends ConditionalTestType
case class NominalMultiwayTestType() extends ConditionalTestType
case class NumericBinaryRulePredicateType() extends ConditionalTestType

trait ConditionalTest extends Serializable {
  def branch(example: Example): Int
  def hasResult(example: Example): Boolean = { branch(example) >= 0 }
  def maxBranches(): Int
}

case class NumericBinaryTest(val fIndex: Int, val value: Double, val isequalTest: Boolean) extends ConditionalTest with Serializable {

  override def branch(example: Example): Int = {
    // todo process missing value
    val v = example.featureAt(fIndex)
    if (v == value) {
      if (isequalTest) 0 else 1
    } else {
      if (v < value) 0 else 1
    }
  }

  override def maxBranches(): Int = 2
}

case class NominalBinaryTest(val fIndex: Int, val value: Double) extends ConditionalTest with Serializable {

  override def branch(example: Example): Int = {
    // todo process missing value
    if (example.featureAt(fIndex) < value) 0 else 1
  }

  override def maxBranches(): Int = 2
}

case class NominalMultiwayTest(val fIndex: Int) extends ConditionalTest with Serializable {

  override def branch(example: Example): Int = {
    // todo process missing value
    example.featureAt(fIndex).toInt
  }

  override def maxBranches(): Int = -1
}

case class NumericBinaryRulePredicate(val fIndex: Int, val value: Double, val operator: Int) extends ConditionalTest with Serializable {
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