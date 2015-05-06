package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite

class FeatureSplitSuite extends FunSuite {

  test("test compare") {
    val fs1: FeatureSplit = new FeatureSplit(null, 1, null)
    val fs2: FeatureSplit = new FeatureSplit(null, 2, null)
    val fs3: FeatureSplit = new FeatureSplit(null, 2, null)
    assert(fs1 < fs2)
    assert(fs2 > fs1)
    assert(fs3 == fs3)
  }

  test("test sort") {
    var fss: Array[FeatureSplit] = new Array[FeatureSplit](4)
    fss(0) = new FeatureSplit(null, 3, null)
    fss(1) = new FeatureSplit(null, 2, null)
    fss(2) = new FeatureSplit(null, 2, null)
    fss(3) = new FeatureSplit(null, 4, null)

    fss = fss.sorted
    for (i <- 0 until fss.length - 1)
      assert(fss(i) <= fss(i + 1))
    assert(fss(0) <= fss(1))
    assert(fss(0) >= fss(1))
    assert(fss(2).merit == 3)

  }
}