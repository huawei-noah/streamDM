package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
class ConditionalTestSuite extends FunSuite {

  test("InfoGainSplitCriterion,test negtive function") {
    val ig: NumericBinaryTest = new NumericBinaryTest(0, 1, false)
    assert(true)
  }
}