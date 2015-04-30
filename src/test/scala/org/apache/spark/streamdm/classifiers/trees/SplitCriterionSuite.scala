package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import org.apache.spark.streamdm.classifiers.trees._
import scala.math.{ log, abs }
class SplitCriterionSuite extends FunSuite {

  test("InfoGainSplitCriterion,test negtive function") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.negtive(Array(0, -1.0, 2)))
  }

  test("InfoGainSplitCriterion,test entropy of array") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.entropy(Array[Double](2.0, 4, 8)) == (log(2.0 + 4 + 8) - (2.0 * log(2.0) + 4 * log(4.0) + 8 * log(8.0)) / (2.0 + 4 + 8)))
    assert(abs(ig.entropy(Array(0.5, 0.5)) - 0.6931471805599453) < 0.00001)
    assert(abs(ig.entropy(Array(3.0, 3)) - 0.6931471805599453) < 0.00001)
  }

  test("InfoGainSplitCriterion, test entropy of matrix ") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(abs(ig.entropy(Array(Array(1.0, 1, 1), Array(1.0, 1, 1))) - 0.6931471805599453) < 0.00001)
  }

  test("InfoGainSplitCriterion, test nunFrac") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.numFrac(Array(new Array(0)), 0.01) == 0)
    assert(ig.numFrac(Array(Array(1, -1, 1), Array(-1, 1, 0)), 0.01) == 1)
    assert(ig.numFrac(Array(Array(1, 1, 1), Array(-1, 1, 1)), 0.01) == 2)
  }
  test("InfoGainSplitCriterion, test getRangeMerit") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.rangeMerit(Array(1.0, 1)) == log(2))
  }
  test("InfoGainSplitCriterion, test getMerit") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.merit(Array(1.0, 1, 1), Array(Array(1, -1, 1), Array(-1, 1, 0))) == Double.NegativeInfinity)
  }
}