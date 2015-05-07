package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.util.Util

/**
 * Trait for observing the class distribution of one feature.
 * The observer monitors the class distribution of a given feature.
 * Used in naive bayes and decision trees to monitor data statistics on leaves.
 */
trait FeatureClassObserver extends Serializable {

  /**
   * Updates statistics of this observer given a feature value, a class index
   * and the weight of the example observed
   *
   * @param cIndex the index of class
   * @param fValue the value of the feature
   * @param weight the weight of the example
   */
  def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit

  /**
   * Gets the probability for an attribute value given a class
   *
   * @param cIndex the index of class
   * @param fValue the value of the feature
   * @return probability for a feature value given a class
   */
  def probability(cIndex: Double, fValue: Double): Double
  /**
   * Gets the best split suggestion given a criterion and a class distribution
   *
   * @param criterion the split criterion to use
   * @param pre the class distribution before the split
   * @param fValue the value of the feature
   * @param isBinarySplit true to use binary splits
   * @return suggestion of best feature split
   */
  def bestSplit(criterion: SplitCriterion, pre: Array[Double], fValue: Double, isBinarySplit: Boolean): FeatureSplit
  // not supported yet
  def observeTarget(fValue: Double, weight: Double): Unit = {}

}

class NullFeatureClassObserver extends FeatureClassObserver with Serializable {

  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {}

  override def probability(cIndex: Double, fValue: Double): Double = 0.0

  override def bestSplit(criterion: SplitCriterion, pre: Array[Double], fValue: Double, isBinarySplit: Boolean): FeatureSplit = { null }
}
/**
 * Trait for observing the class distribution of a nominal feature.
 * The observer monitors the class distribution of a given feature.
 * Used in naive bayes and decision trees to monitor data statistics on leaves.
 */
class NominalFeatureClassObserver(val numClasses: Int, val numFeatureValues: Int, val fIndex: Int, laplaceSmoothingFactor: Int = 1) extends FeatureClassObserver with Serializable {

  var classFeatureStatistics: Array[Array[Double]] = Array.fill(numClasses)(new Array[Double](numFeatureValues))

  var totalWeight: Double = 0.0
  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {
    classFeatureStatistics(cIndex.toInt)(fValue.toInt) += weight
    totalWeight += weight
  }

  override def probability(cIndex: Double, fValue: Double): Double = {
    val sum = classFeatureStatistics(cIndex.toInt).sum
    if (sum == 0) 0.0 else {
      (classFeatureStatistics(cIndex.toInt)(fValue.toInt) + laplaceSmoothingFactor) /
        (sum + numFeatureValues * laplaceSmoothingFactor)
    }
  }

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

  private[trees] def binarySplit(fValue: Double): Array[Array[Double]] =
    { Util.splitTranspose(classFeatureStatistics, fValue.toInt) }

  private[trees] def multiSplit(fValue: Double): Array[Array[Double]] =
    { Util.transpose(classFeatureStatistics) }
}
/**
 * Class for observing the class data distribution for a numeric feature using gaussian estimators.
 * This observer monitors the class distribution of a given feature.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 */
class GuassianNumericFeatureClassOberser(val numClasses: Int, val fIndex: Int) extends FeatureClassObserver with Serializable {

  val estimators: Array[GaussianEstimator] = new Array[GaussianEstimator](numClasses)
  
  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {}

  override def probability(cIndex: Double, fValue: Double): Double = 0.0

  override def bestSplit(criterion: SplitCriterion, pre: Array[Double],
                         fValue: Double, isBinarySplit: Boolean): FeatureSplit = { null }
}

object FeatureClassObserver {
  def createFeatureClassObserver(featureType: FeatureType, numClasses: Int,
                                 fIndex: Int, numFeatureValues: Int = 0): FeatureClassObserver = featureType match {
    case nominal: NominalFeatureType => new NominalFeatureClassObserver(numClasses, numFeatureValues, fIndex)
    case numeric: NumericFeatureType => new GuassianNumericFeatureClassOberser(numClasses, fIndex)
    case _: NullFeatureType          => new NullFeatureClassObserver
  }
}