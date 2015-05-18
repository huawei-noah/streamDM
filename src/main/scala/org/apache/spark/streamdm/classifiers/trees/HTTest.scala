package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ log }
import scala.util.Random
import org.apache.spark.streamdm.core._

object HTTest {
  def main(args: Array[String]) {
    testHTM()
  }
  def testHTM() {
    val featureTypes: Array[FeatureType] = Array[FeatureType](new NominalFeatureType(10), new NominalFeatureType(10), new NominalFeatureType(10), new NominalFeatureType(10))
    val htm: HoeffdingTreeModel = new HoeffdingTreeModel(false, 3, 4, new FeatureTypeArray(featureTypes))

    val examples = randomExample(3, 4, 10, 10000)
    examples.foreach { x => htm.update(x) }
    println("Hoeffding Tree")
    println(htm.root)
    println(htm.root.sum())
    println(htm.activeNodeCount)
    println(htm.inactiveNodeCount)
    println(htm.deactiveNodeCount)
    println(htm.decisionNodeCount)
    println(htm.blockNumExamples)
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