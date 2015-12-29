package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import org.apache.spark.streamdm.classifiers.trees._
import scala.math.{ log, abs }
import org.apache.spark.streamdm.utils.Utils.{ log2 }
class SplitCriterionSuite extends FunSuite {

  test("InfoGainSplitCriterion, test negtive function") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.hasNegative(Array(0, -1.0, 2)))
  }

  test("InfoGainSplitCriterion, test entropy of array") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.entropy(Array[Double](2.0, 4, 8)) == (log2(2.0 + 4 + 8) - (2.0 * log2(2.0) + 4 * log2(4.0) + 8 * log2(8.0)) / (2.0 + 4 + 8)))
    assert(ig.entropy(Array(0.5, 0.5)) == 1.0)
    assert(ig.entropy(Array(0.4, 0.6)) == 0.9709505944546686)
  }

  test("InfoGainSplitCriterion, test entropy of matrix ") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.entropy(Array(Array(1.0, 1, 1), Array(1.0, 1, 1))) == 1.5849625007211563)
  }

  test("InfoGainSplitCriterion, test nunFrac") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.numGTFrac(Array(new Array(0)), 0.01) == 0)
    assert(ig.numGTFrac(Array(Array(1, -1, 1), Array(-1, 1, 0)), 0.01) == 1)
    assert(ig.numGTFrac(Array(Array(1, 1, 1), Array(-1, 1, 1)), 0.01) == 2)
    assert(ig.numGTFrac(Array(Array(1, 2, 3, 4, 5), Array(5, 4, 3, 2, 1)), 0.01) == 2)
  }
  test("InfoGainSplitCriterion, test rangeMerit") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.rangeMerit(Array(1.0, 1)) == log2(2))
  }
  test("InfoGainSplitCriterion, test merit") {
    val ig: InfoGainSplitCriterion = new InfoGainSplitCriterion()
    assert(ig.merit(Array(1.0, 1, 1), Array(Array(1, -1, 1), Array(-1, 1, 0))) == Double.NegativeInfinity)
  }

  test("GiniSplitCriterion, test computeGini") {
    val gc: GiniSplitCriterion = new GiniSplitCriterion()
    assert(gc.computeGini(Array[Double](1, 1, 1), 3) == 1.0 - 3.0 * 1.0 / 9)
  }
  test("GiniSplitCriterion, test merit") {
    val gc: GiniSplitCriterion = new GiniSplitCriterion()
    assert(abs(gc.merit(Array(1.0 / 3, 1.0 / 3, 1.0 / 3),
      Array(Array(1.0 / 6, 1.0 / 6, 1.0 / 6), Array(1.0 / 6, 1.0 / 6, 1.0 / 6))) - 0.33333333333) < 0.000001)
  }

}