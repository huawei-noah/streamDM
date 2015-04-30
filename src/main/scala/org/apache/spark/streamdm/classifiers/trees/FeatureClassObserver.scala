package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.util.Util

trait FeatureClassObserver extends Serializable {
  def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit
  def observeTarget(fValue: Double, weight: Double): Unit
  def probability(cIndex: Double, fValue: Double): Double
  def bestSplit(criterion: SplitCriterion, pre: Array[Double], fValue: Double, isBinarySplit: Boolean): FeatureSplit
}

class NullFeatureClassObserver extends FeatureClassObserver with Serializable {
  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {}
  override def observeTarget(fValue: Double, weight: Double): Unit = {}
  override def probability(cIndex: Double, fValue: Double): Double = 0.0
  override def bestSplit(criterion: SplitCriterion, pre: Array[Double], fValue: Double, isBinarySplit: Boolean): FeatureSplit = { null }
}

class NominalFeatureClassObserver(val numClasses: Int, val numFeatureValues: Int, val fIndex: Int) extends FeatureClassObserver with Serializable {
  var classFeatureStatistics: Array[Array[Double]] = Array.fill(numClasses)(new Array[Double](numFeatureValues))
  var totalWeight: Double = 0.0
  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {
    classFeatureStatistics(cIndex.toInt)(fValue.toInt) += weight
    totalWeight += weight
  }
  override def observeTarget(fValue: Double, weight: Double): Unit = {
    // not support yet
  }
  override def probability(cIndex: Double, fValue: Double): Double = 0.0
  override def bestSplit(criterion: SplitCriterion, pre: Array[Double],
                         fValue: Double, isBinarySplit: Boolean): FeatureSplit = {
    var fSplit: FeatureSplit = null
    for (i <- 0 until pre.length) {
      val post: Array[Array[Double]] = binarySplit(i)
      val merit = criterion.merit(pre, post)
      if (fSplit == null || fSplit.merit < merit)
        fSplit = new FeatureSplit(new NominalBinaryTest(fIndex, i), merit, post)
    }
    if (!isBinarySplit) {
      val post = multiSplit(0.0)
      val merit = criterion.merit(pre, post)
      if (fSplit.merit < merit)
        fSplit = new FeatureSplit(new NominalMultiwayTest(fIndex), merit, post)
    }
    fSplit
  }

  def binarySplit(fValue: Double): Array[Array[Double]] = { Util.splitTranspose(classFeatureStatistics, fValue.toInt) }
  def multiSplit(fValue: Double): Array[Array[Double]] = { Util.transpose(classFeatureStatistics) }
}
// todo NumericFeatureClassObersers, GuassianNumericFeatureClassOberser and so on

object FeatureClassObserver {
  def createFeatureClassObserver(featureType: FeatureType, numClasses: Int, numFeatureValues: Int, fIndex: Int): FeatureClassObserver = featureType match {
    case o: NominalFeatureType => new NominalFeatureClassObserver(numClasses, numFeatureValues, fIndex)
  }
}