package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import scala.math.{abs}
class GaussianEstimatorSuite extends FunSuite {

  test("test observe,stddev") {
    val gs: GaussianEstimator = new GaussianEstimator()
    gs.observe(2.0, 0.5)
    assert(gs.getMean() == 2.0)
    assert(gs.totalWeight() == 0.5)
    gs.observe(1, 2)
    assert(gs.totalWeight() == 2.5)
    assert(gs.getMean() == 1.2)
    assert(gs.stdDev() == 0.5163977794943222)
    assert(gs.variance() == 0.2666666666666666)
  }

  test("test variance") {
    val gs: GaussianEstimator = new GaussianEstimator()
    gs.observe(2.0, 0.5)
    assert(gs.variance() == 0)
    gs.observe(1, 0.5)
    assert(gs.variance() == 0)
    gs.observe(3, 0.5)
    assert(gs.variance() == 2.0)
  }

  test("test probalilityDensity") {
    val gs: GaussianEstimator = new GaussianEstimator()
    assert(gs.probabilityDensity(0) == 0)
    assert(gs.probabilityDensity(1) == 0)
    assert(gs.probabilityDensity(2) == 0)
    gs.observe(2.0, 1)
    assert(gs.probabilityDensity(0) == 0)
    assert(gs.probabilityDensity(1) == 0)
    assert(gs.probabilityDensity(2) == 1)
    gs.observe(1, 2)
    assert(gs.probabilityDensity(gs.getMean()) == 0.690988298942671)
    assert(gs.probabilityDensity(1) == 0.5849089671682234)
    assert(abs(gs.probabilityDensity(2) -0.3547652217283777)<0.0000001)
  }

  test("test merge") {
    val gs1: GaussianEstimator = new GaussianEstimator()
    gs1.observe(2.0, 1)
    gs1.observe(1.0, 2.0)
    val gs2: GaussianEstimator = new GaussianEstimator()
    gs2.observe(3.0, 1)
    gs2.observe(1.0, 3.0)
    val merge: GaussianEstimator = gs1.merge(gs2);
    val gs3: GaussianEstimator = new GaussianEstimator()
    gs3.observe(2.0, 1)
    gs3.observe(1.0, 2.0)
    gs3.observe(3.0, 1)
    gs3.observe(1.0, 3.0)
    assert(merge.getMean() == gs3.getMean())
    assert(merge.totalWeight() == gs3.totalWeight())
    assert(merge.stdDev() == merge.stdDev())
    assert(merge.variance() == merge.variance())
  }

}