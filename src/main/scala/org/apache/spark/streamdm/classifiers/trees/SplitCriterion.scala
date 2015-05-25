package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ log, max }

trait SplitCriterionType

case class InfoGainSplitCriterionType() extends SplitCriterionType

case class GiniSplitCriterionType() extends SplitCriterionType

case class VarianceReductionSplitCriterionType() extends SplitCriterionType

/**
 * trait SplitCriterion for computing splitting criteria with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
 * The two split criteria most used are Information Gain and Gini.
 */

trait SplitCriterion extends Serializable {
  /**
   * Computes the merit of splitting for a given distribution before and after the split.
   *
   * @param pre the class distribution before the split
   * @param post the class distribution after the split
   * @return value of the merit of splitting
   */
  def merit(pre: Array[Double], post: Array[Array[Double]]): Double

  /**
   * Computes the range of splitting merit
   *
   * @param pre the class distribution before the split
   * @return value of the range of splitting merit
   */

  def rangeMerit(pre: Array[Double]): Double

}
/**
 * trait SplitCriterion for computing splitting criteria using Information Gain
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
 */

class InfoGainSplitCriterion extends SplitCriterion with Serializable {

  var minBranch: Double = 0.01

  def this(minBranch: Double) {
    this()
    this.minBranch = minBranch
  }
  
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    if (numFrac(post, minBranch) < 2) Double.NegativeInfinity
    else entropy(pre) - entropy(post)
  }

  override def rangeMerit(pre: Array[Double]): Double = log(max(pre.length, 2))

  def entropy(pre: Array[Double]): Double = {
    if (pre == null || pre.sum <= 0 || hasNegative(pre)) 0.0
    log(pre.sum) - pre.filter(_ > 0).map(x => x * log(x)).sum / pre.sum
  }

  def entropy(post: Array[Array[Double]]): Double = {
    if (post == null || post.length == 0 || post(0).length == 0) 0
    val tpost: Array[Array[Double]] = Array.fill(post(0).length)(new Array[Double](post.length))
    post.zipWithIndex.map {
      x => { x._1.zipWithIndex.map { y => tpost(y._2)(x._2) = post(x._2)(y._2) } }
    }
    tpost.map { x => entropy(x) * x.sum }.sum / post.map(_.sum).sum
  }

  private[trees] def numFrac(post: Array[Array[Double]], minFrac: Double): Int = {
    if (post == null || post.length == 0) 0
    val sums: Array[Double] = new Array[Double](post(0).length)
    post.map { _.zipWithIndex.map { x => sums(x._2) += x._1 } }
    sums.filter(_ > sums.sum * minFrac).length
  }
  /*
   * check whether a array has a negative value
   */
  private[trees] def hasNegative(pre: Array[Double]): Boolean = pre.filter(x => x < 0).length > 0

}

/**
 * trait SplitCriterion for computing splitting criteria using Gini
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
 */

class GiniSplitCriterion extends SplitCriterion with Serializable {

  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    val sums = post.map(_.sum)
    val totalWeight = sums.sum
    val ginis: Array[Double] = post.zip(sums).map {
      case (x, y) => computeGini(x, y) * y / totalWeight
    }
    1.0 - ginis.sum
  }

  private[trees] def computeGini(dist: Array[Double], sum: Double): Double =
    1.0 - dist.map { x => x * x / sum / sum }.sum

  override def rangeMerit(pre: Array[Double]): Double = 1.0

}

class VarianceReductionSplitCriterion extends SplitCriterion with Serializable {
  //todo
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = 0

  override def rangeMerit(pre: Array[Double]): Double = 0.0
}

object SplitCriterion {
  /*
   * return a new SplitCriterion, the default will be InfoGainSplitCriterion.
   */
  def createSplitCriterion(
    scType: SplitCriterionType, minBranch: Double = 0.01): SplitCriterion = scType match {
    case infoGrain: InfoGainSplitCriterionType   => new InfoGainSplitCriterion(minBranch)
    case gini: GiniSplitCriterionType            => new GiniSplitCriterion()
    case vr: VarianceReductionSplitCriterionType => new VarianceReductionSplitCriterion()
    case _                                       => new InfoGainSplitCriterion(minBranch)
  }
}