package org.apache.spark.streamdm.classifiers.bayes

//import org.apache.spark.streamdm.classifiers.bayes.MultinomialNaiveBayes
import org.apache.spark.streamdm.classifiers.bayes._
import org.apache.spark.streamdm.core._

import org.scalatest.FunSuite

/**
 * Test suite for the Multinomial NaiveBayes.
 */
class MultinomialNaiveBayesSuite extends FunSuite {

  test("the merge function must merge statistics and return a new model") {
    val ct1: Array[Double] = Array(1, 0)
    val ct2: Array[Double] = Array(0, 1)
    val cfs1: Array[Array[Double]] = Array(Array(1.0, 0, 0), Array(0, 2, 0))
    val cfs2: Array[Array[Double]] = Array(Array(-1, 0, 0), Array(0, -2, 0))
    val mod1 = new MultinomialNaiveBayesModel(2, 3, 1, ct1, cfs1)
    val mod2 = new MultinomialNaiveBayesModel(2, 3, 1, ct2, cfs2)
    val mergemodel = mod1.merge(mod2)
    assert(mod1 != mergemodel)

    assert(mergemodel.classStatistics(0) == 1.0)
    assert(mergemodel.classStatistics(1) == 1.0)
    mergemodel.classFeatureStatistics.foreach { x => x.foreach { y => assert(y == 0) } }

  }

  test("the create function must init as the input") {
    val ct1: Array[Double] = Array(1, 0)
    val ct2: Array[Double] = Array(0, 1)
    val cfs1: Array[Array[Double]] = Array(Array(1.0, 0, 0), Array(0, 2, 0))
    val cfs2: Array[Array[Double]] = Array(Array(-1, 0, 0), Array(0, -2, 0))
    val mod1 = new MultinomialNaiveBayesModel(2, 3, 1)
    val mod2 = new MultinomialNaiveBayesModel(2, 3, 1, ct2, cfs2)
    assert(mod1.numClasses == 2)
    assert(mod1.numFeatures == 3)
    assert(mod1.laplaceSmoothingFactor == 1)

    assert(mod1.classStatistics(0) == 0)
    assert(mod1.classStatistics(1) == 0)
    mod1.classFeatureStatistics.foreach { x => x.foreach { y => assert(y == 0) } }
    assert(mod2.classStatistics == ct2)
    assert(mod2.classFeatureStatistics == cfs2)

  }

  test("predict must return the right label") {
    val ct1: Array[Double] = Array(1, 1)
    val cfs1: Array[Array[Double]] = Array(Array(1, 0), Array(0, 1))
    val mod1 = new MultinomialNaiveBayesModel(2, 2, 1, ct1, cfs1)
    val instance: Example = Example.parse("1 1,1", "dense", "dense")

    assert(mod1.predict(instance) == 0.0)
    val instance1: Example = Example.parse("0 1,0", "dense", "dense")
    assert(mod1.predict(instance1) == 0.0)
    val instance2: Example = Example.parse("1 0,1", "dense", "dense")
    assert(mod1.predict(instance2) == 1)
  }

  test("updated labeled data must be added to the statistics") {
    val ct1: Array[Double] = Array(1, 1)
    val cfs1: Array[Array[Double]] = Array(Array(1, 0), Array(0, 1))
    val mod1 = new MultinomialNaiveBayesModel(2, 2, 1, ct1, cfs1)

    val instance1: Example = Example.parse("0 1,0", "dense", "dense")
    mod1.update(instance1)
    assert(mod1.classStatistics(0) == 2)
    assert(mod1.classStatistics(1) == 1)
    assert(mod1.classFeatureStatistics(0)(0) == 2)
    assert(mod1.classFeatureStatistics(0)(1) == 0)
    assert(mod1.classFeatureStatistics(1)(0) == 0)
    assert(mod1.classFeatureStatistics(1)(1) == 1)
    val instance2: Example = Example.parse("1 0,1", "dense", "dense")
    mod1.update(instance2)
    assert(mod1.classStatistics(0) == 2)
    assert(mod1.classStatistics(1) == 2)
    assert(mod1.classFeatureStatistics(0)(0) == 2)
    assert(mod1.classFeatureStatistics(0)(1) == 0)
    assert(mod1.classFeatureStatistics(1)(0) == 0)
    assert(mod1.classFeatureStatistics(1)(1) == 2)
  }

}