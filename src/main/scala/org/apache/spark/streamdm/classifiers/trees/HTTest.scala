package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ log, log10, log1p }
import scala.util.Random
import org.apache.spark.streamdm.core._

object HTTest {
  def main(args: Array[String]): Unit = {
    //testHTM()
    //testInfoGain()
    //    println(log10(2))
    //    println(log(2))
    //    println(log1p(2))
    testGE()
  }

  def testInfoGain(): Unit = {
    val infoGain = new InfoGainSplitCriterion()
    val pre = Array[Double](8785.0, 7614.0)
    val post = Array(Array(1291.0, 0.0), Array(7638.0, 7614.0))
    val merit: Double = infoGain.merit(pre, post)
    println(infoGain.entropy(pre))
    println(infoGain.entropy(post))
    println(infoGain.entropy(Array[Double](2.0, 4, 8)))
    println(infoGain.entropy(Array(0.4, 0.6)))
    println(infoGain.entropy(Array(Array(1.0, 1, 1), Array(1.0, 1, 1))))
    println(merit)
  }

  def testHTM() {
    val featureTypes: Array[FeatureType] = Array[FeatureType](new NominalFeatureType(10), new NominalFeatureType(10), new NominalFeatureType(10), new NominalFeatureType(10))
    val htm: HoeffdingTreeModel = new HoeffdingTreeModel(false, 3, 4, new FeatureTypeArray(featureTypes))

    val examples = randomExample(3, 4, 10, 10000)
    examples.foreach { x => htm.update(x) }
    println("Hoeffding Tree")
    println(htm.root)
    println(htm.activeNodeCount)
    println(htm.inactiveNodeCount)
    println(htm.deactiveNodeCount)
    println(htm.decisionNodeCount)
    println(htm.blockNumExamples)
  }

  def testGE() = {
    val gs: GaussianEstimator = new GaussianEstimator()
    gs.observe(2.0, 0.5)
    gs.observe(1, 0.5)
    gs.observe(3, 0.5)
    var gs2: GaussianEstimator = new GaussianEstimator()
    gs2 = gs2.merge(gs, false)
    println(gs2.variance())
    //    assert(gs2.variance() == 0.2666666666666666)

  }

  def randomExample(numClasses: Int, numFeatures: Int, valueRange: Int, num: Int): Array[Example] = {
    val rst: Array[Example] = new Array[Example](num)
    val random = new Random()
    for (i <- 0 until num) {
      val labels: Array[Double] = new Array[Double](1)
      labels(0) = random.nextInt(numClasses)
      val features: Array[Double] = new Array[Double](numFeatures)
      for (j <- 0 until numFeatures) {
        features(j) = random.nextInt(valueRange)
      }
      rst(i) = new Example(new DenseInstance(features), new DenseInstance(labels))
    }
    rst
  }
}