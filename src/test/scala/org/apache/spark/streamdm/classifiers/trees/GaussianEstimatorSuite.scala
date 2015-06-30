package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import scala.math.{ abs }
class GaussianEstimatorSuite extends FunSuite {

  test("test observe,stddev,variance,merge") {
    val gs: GaussianEstimator = new GaussianEstimator()
    gs.observe(2.0, 0.5)
    gs.observe(1, 2)
    var gs2: GaussianEstimator = new GaussianEstimator()
    gs2 = gs2.merge(gs, true)
    assert(gs2.totalWeight() == 2.5)
    assert(gs2.getMean() == 1.2)
    assert(gs2.stdDev() == 0.5163977794943222)
    assert(gs2.variance() == 0.2666666666666666)
  }

  test("test variance") {
    val gs: GaussianEstimator = new GaussianEstimator()
    gs.observe(2.0, 0.5)
    gs.observe(1, 0.5)
    gs.observe(3, 0.5)
    var gs2: GaussianEstimator = new GaussianEstimator()
    gs2 = gs2.merge(gs, true)
    assert(gs2.variance() == 2.0)
  }

  test("test probalilityDensity") {
    val gs: GaussianEstimator = new GaussianEstimator()
    assert(gs.probabilityDensity(0) == 0)
    assert(gs.probabilityDensity(1) == 0)
    assert(gs.probabilityDensity(2) == 0)
    gs.observe(2.0, 1)
    var gs2: GaussianEstimator = new GaussianEstimator()
    gs2 = gs2.merge(gs, true)
    assert(gs2.probabilityDensity(0) == 0)
    assert(gs2.probabilityDensity(1) == 0)
    assert(gs2.probabilityDensity(2) == 1)
  }

}