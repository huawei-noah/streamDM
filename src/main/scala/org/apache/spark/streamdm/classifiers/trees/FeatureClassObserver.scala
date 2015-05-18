package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streamdm.util.Util
import scala.math.{ min, max }
import scala.collection.mutable.TreeSet

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

  def blockMerge(that: FeatureClassObserver): FeatureClassObserver

  // not supported yet
  def observeTarget(fValue: Double, weight: Double): Unit = {}

}

class NullFeatureClassObserver extends FeatureClassObserver with Serializable {

  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {}

  override def probability(cIndex: Double, fValue: Double): Double = 0.0

  override def bestSplit(criterion: SplitCriterion, pre: Array[Double], fValue: Double, isBinarySplit: Boolean): FeatureSplit = { null }

  override def blockMerge(that: FeatureClassObserver): FeatureClassObserver = this
}
/**
 * Trait for observing the class distribution of a nominal feature.
 * The observer monitors the class distribution of a given feature.
 * Used in naive bayes and decision trees to monitor data statistics on leaves.
 */
class NominalFeatureClassObserver(val numClasses: Int, val fIndex: Int, val numFeatureValues: Int, val laplaceSmoothingFactor: Int = 1) extends FeatureClassObserver with Serializable {

  val baseClassFeatureStatistics: Array[Array[Double]] = Array.fill(numClasses)(new Array[Double](numFeatureValues))

  val blockClassFeatureStatistics: Array[Array[Double]] = Array.fill(numClasses)(new Array[Double](numFeatureValues))

  var totalWeight: Double = 0.0
  var blockWeight: Double = 0.0

  def this(that: NominalFeatureClassObserver) {
    this(that.numClasses, that.fIndex, that.numFeatureValues, that.laplaceSmoothingFactor)
    for (i <- 0 until numClasses; j <- 0 until numFeatureValues)
      baseClassFeatureStatistics(i)(j) = that.baseClassFeatureStatistics(i)(j) + that.blockClassFeatureStatistics(i)(j)
    totalWeight = that.totalWeight + that.blockWeight
  }

  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {
    blockClassFeatureStatistics(cIndex.toInt)(fValue.toInt) += weight
    blockWeight += weight
  }

  override def probability(cIndex: Double, fValue: Double): Double = {
    val sum = baseClassFeatureStatistics(cIndex.toInt).sum
    if (sum == 0) 0.0 else {
      (baseClassFeatureStatistics(cIndex.toInt)(fValue.toInt) + laplaceSmoothingFactor) /
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
      val post = multiwaySplit(0.0)
      val merit = criterion.merit(pre, post)
      if (fSplit.merit < merit)
        fSplit = new FeatureSplit(new NominalMultiwayTest(fIndex), merit, post)
    }
    fSplit
  }
  /*
   * merge the blockClassFeatureStatistics of that with the baseClassFeatureStatistics of this
   * return current NominalFeatureClassObserver
   */
  override def blockMerge(that: FeatureClassObserver): FeatureClassObserver = {
    if (!that.isInstanceOf[NominalFeatureClassObserver])
      this
    else {
      val observer = that.asInstanceOf[NominalFeatureClassObserver]
      if (numClasses != observer.numClasses || fIndex != observer.fIndex ||
        numFeatureValues != observer.numFeatureValues ||
        laplaceSmoothingFactor != observer.laplaceSmoothingFactor) this
      else {
        totalWeight += observer.blockWeight
        for (i <- 0 until baseClassFeatureStatistics.length; j <- 0 until baseClassFeatureStatistics(i).length)
          baseClassFeatureStatistics(i)(j) += observer.blockClassFeatureStatistics(i)(j)
        this
      }
    }
  }

  private[trees] def binarySplit(fValue: Double): Array[Array[Double]] =
    { Util.splitTranspose(baseClassFeatureStatistics, fValue.toInt) }

  private[trees] def multiwaySplit(fValue: Double): Array[Array[Double]] =
    { Util.transpose(baseClassFeatureStatistics) }
}
/**
 * Class for observing the class data distribution for a numeric feature using gaussian estimators.
 * This observer monitors the class distribution of a given feature.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 */
class GuassianNumericFeatureClassOberser(val numClasses: Int, val fIndex: Int, val numBins: Int = 10) extends FeatureClassObserver with Serializable {

  val estimators: Array[GaussianEstimator] = Array.fill(numClasses)(new GaussianEstimator())
  val minValuePerClass: Array[Double] = Array.fill(numClasses)(Double.PositiveInfinity)
  val maxValuePerClass: Array[Double] = Array.fill(numClasses)(Double.NegativeInfinity)

  def this(that: GuassianNumericFeatureClassOberser) {
    this(that.numClasses, that.fIndex, that.numBins)
    //todo
  }

  override def observeClass(cIndex: Double, fValue: Double, weight: Double): Unit = {
    if (false) {
      // todo, process missing value

    } else {

      if (minValuePerClass(cIndex.toInt) > fValue)
        minValuePerClass(cIndex.toInt) = fValue
      if (maxValuePerClass(cIndex.toInt) < fValue)
        maxValuePerClass(cIndex.toInt) = fValue
      estimators(cIndex.toInt).observe(fValue, weight)
    }
  }

  override def probability(cIndex: Double, fValue: Double): Double = {
    if (estimators(cIndex.toInt) == null) 0.0
    else estimators(cIndex.toInt).probabilityDensity(fValue)
  }

  override def bestSplit(criterion: SplitCriterion, pre: Array[Double],
                         fValue: Double, isBinarySplit: Boolean): FeatureSplit = {
    var fSplit: FeatureSplit = null
    val points: Array[Double] = splitPoints()
    for (splitValue: Double <- points) {
      val post: Array[Array[Double]] = binarySplit(splitValue)
      val merit = criterion.merit(pre, post)
      if (fSplit == null || fSplit.merit < merit)
        fSplit = new FeatureSplit(new NumericBinaryTest(fIndex, splitValue, true), merit, post)
    }
    fSplit
  }
  //values equal to splitValue go to left 
  private[trees] def binarySplit(splitValue: Double): Array[Array[Double]] = {
    val rst: Array[Array[Double]] = Array.fill(2)(new Array(numClasses))
    estimators.zipWithIndex.foreach {
      case (es, i) => {
        if (splitValue < minValuePerClass(i)) {
          rst(1)(i) += es.totalWeight()
        } else if (splitValue >= maxValuePerClass(i)) {
          rst(0)(i) += es.totalWeight()
        } else {
          val weights: Array[Double] = es.tripleWeights(splitValue)
          rst(0)(i) += weights(0) + weights(1)
          rst(1)(i) += weights(2)
        }
      }
    }
    rst
  }

  private[trees] def splitPoints(): Array[Double] = {
    var minValue = Double.PositiveInfinity
    var maxValue = Double.NegativeInfinity
    val points = new TreeSet[Double]()
    minValuePerClass.foreach { x => minValue = min(minValue, x) }
    maxValuePerClass.foreach { x => maxValue = max(maxValue, x) }
    if (minValue < Double.PositiveInfinity) {
      val range = maxValue - minValue
      for (i <- 0 until numBins) {
        val splitValue = range * (i + 1) / (numBins) + minValue
        if (splitValue > minValue && splitValue < maxValue)
          points.add(splitValue)
      }
    }
    points.toArray
  }
  // todo
  override def blockMerge(that: FeatureClassObserver): FeatureClassObserver = this
}

object FeatureClassObserver {
  def createFeatureClassObserver(featureType: FeatureType, numClasses: Int,
                                 fIndex: Int, numFeatureValues: Int = 0): FeatureClassObserver = featureType match {
    case nominal: NominalFeatureType => new NominalFeatureClassObserver(numClasses, fIndex, numFeatureValues)
    case numeric: NumericFeatureType => new GuassianNumericFeatureClassOberser(numClasses, fIndex)
    case _: NullFeatureType          => new NullFeatureClassObserver
  }

  def createFeatureClassObserver(observer: FeatureClassObserver): FeatureClassObserver = observer match {
    case nominal: NominalFeatureClassObserver => new NominalFeatureClassObserver(nominal)
    case _: NullFeatureClassObserver          => new NullFeatureClassObserver
  }

}