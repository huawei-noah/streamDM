package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ log }
import scala.util.Random
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.trees.FeatureSplit

object HTTest {
  def main(args: Array[String]) {
//    var fss: Array[FeatureSplit] = new Array[FeatureSplit](4)
//    fss(0) = new FeatureSplit(null, 3, null)
//    fss(1) = new FeatureSplit(null, 2, null)
//    fss(2) = new FeatureSplit(null, 2, null)
//    fss(3) = new FeatureSplit(null, 4, null)
//    fss.sorted
//    //fss = fss.sorted
//    fss.foreach(x => println(x.merit))
//
//    println(fss(0) == fss(1))
//    println(fss(2) == new FeatureSplit(null, 3, null))
//
//    for (i <- 0 until fss.length - 1)
//      println(fss(i) <= fss(i + 1))
    testHTM()
  }
  def testHTM() {
    val featureTypes: Array[FeatureType] = Array[FeatureType](new NominalFeatureType, new NominalFeatureType, new NominalFeatureType, new NominalFeatureType)
    val htm: HoeffdingTreeModel = new HoeffdingTreeModel(3, 4, 10, featureTypes)
    val examples = randomExample(3, 4, 10, 100000)
    examples.foreach { x => htm.train(x) }
    println(htm.root)
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