/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.classifiers.trees

import scala.math.max

import org.apache.spark.streamdm.utils.Utils.log2

trait SplitCriterionType

case class InfoGainSplitCriterionType() extends SplitCriterionType

case class GiniSplitCriterionType() extends SplitCriterionType

case class VarianceReductionSplitCriterionType() extends SplitCriterionType

/**
 * Trait for computing splitting criteria with respect to distributions of class values.
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
 * Class for computing splitting criteria using information gain with respect to
 * distributions of class values.
 */
class InfoGainSplitCriterion extends SplitCriterion with Serializable {

  var minBranch: Double = 0.01

  def this(minBranch: Double) {
    this()
    this.minBranch = minBranch
  }

  /**
   * Computes the merit of splitting for a given distribution before and after the split.
   *
   * @param pre the class distribution before the split
   * @param post the class distribution after the split
   * @return value of the merit of splitting
   */
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    val num = numGTFrac(post, minBranch)
    if (numGTFrac(post, minBranch) < 2) Double.NegativeInfinity
    else {
      val merit = entropy(pre) - entropy(post)
      merit
    }
  }

  /**
   * Computes the range of splitting merit
   *
   * @param pre the class distribution before the split
   * @return value of the range of splitting merit
   */
  override def rangeMerit(pre: Array[Double]): Double = log2(max(pre.length, 2))

  /**
   * Returns the entropy of a distribution
    *
   * @param pre an Array containing the distribution
   * @return the entropy
   */
  def entropy(pre: Array[Double]): Double = {
    if (pre == null || pre.sum <= 0 || hasNegative(pre)) {
      0.0
    }
    else {
      log2(pre.sum) - pre.filter(_ > 0).map(x => x * log2(x)).sum / pre.sum
    }
  }

  /**
   * Computes the entropy of an matrix
   *
   * @param post the matrix as an Array of Array
   * @return the entropy
   */
  def entropy(post: Array[Array[Double]]): Double = {
    if (post == null || post.length == 0 || post(0).length == 0) 0.0
    else {
      post.map { row => (row.sum * entropy(row)) }.sum / post.map(_.sum).sum
    }

  }

  /**
   * Returns number of subsets which have values greater than minFrac
   *
   * @param post the matrix as an Array of Array
   * @param minFrac the min threshold
   * @return number of subsets
   */
  def numGTFrac(post: Array[Array[Double]], minFrac: Double): Int = {
    if (post == null || post.length == 0) {
      0
    } else {
      val sums = post.map { _.sum }
      sums.filter(_ > sums.sum * minFrac).length
    }
  }

  /**
   * Returns whether a array has negative value
   *
   * @param pre an Array to be valued
   * @return whether a array has negative value
   */
  private[trees] def hasNegative(pre: Array[Double]): Boolean = pre.filter(x => x < 0).length > 0

}

/**
 * Class for computing splitting criteria using Gini with respect to
 * distributions of class values.
 */

class GiniSplitCriterion extends SplitCriterion with Serializable {

  /**
   * Computes the merit of splitting for a given distribution before and after the split.
   *
   * @param pre the class distribution before the split
   * @param post the class distribution after the split
   * @return value of the merit of splitting
   */
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    val sums = post.map(_.sum)
    val totalWeight = sums.sum
    val ginis: Array[Double] = post.zip(sums).map {
      case (x, y) => computeGini(x, y) * y / totalWeight
    }
    1.0 - ginis.sum
  }

  /**
   * Computes the range of splitting merit
   *
   * @param pre the class distribution before the split
   * @return value of the range of splitting merit
   */
  override def rangeMerit(pre: Array[Double]): Double = 1.0

  /**
   * Computes the gini of an array
   *
   * @param dist an array to be computed
   * @param sum the sum of the array
   * @return the gini of an array
   */
  private[trees] def computeGini(dist: Array[Double], sum: Double): Double =
    1.0 - dist.map { x => x * x / sum / sum }.sum

}

/**
 * Class for computing splitting criteria using variance reduction with respect
 * to distributions of class values.
 */
class VarianceReductionSplitCriterion extends SplitCriterion with Serializable {

  val magicNumber = 5.0

  /**
   * Computes the merit of splitting for a given distribution before and after the split.
   *
   * @param pre the class distribution before the split
   * @param post the class distribution after the split
   * @return value of the merit of splitting
   */
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    val count = post.map { row => if (row(0) >= magicNumber) 1 else 0 }.sum
    if (count != post.length) 0
    else {
      var sdr = computeSD(pre)
      post.foreach { row => sdr -= (row(0) / pre(0)) * computeSD(row) }
      sdr
    }
  }

  /**
   * Computes the range of splitting merit
   *
   * @param pre the class distribution before the split
   * @return value of the range of splitting merit
   */
  override def rangeMerit(pre: Array[Double]): Double = 1.0

  /**
   * Computes the standard deviation of a distribution
   *
   * @param pre an Array containing the distribution
   * @return the standard deviation
   */
  private[trees] def computeSD(pre: Array[Double]): Double = {
    val n = pre(0).toInt
    val sum = pre(1)
    val sumSq = pre(2)
    (sumSq - ((sum * sum) / n)) / n
  }
}

object SplitCriterion {

  /**
   * Return a new SplitCriterion, by default InfoGainSplitCriterion.
   * @param scType the type of the split criterion
   * @param minBranch branch parameter
   * @return the new SplitCriterion
   */
  def createSplitCriterion(
    scType: SplitCriterionType, minBranch: Double = 0.01): SplitCriterion = scType match {
    case infoGrain: InfoGainSplitCriterionType   => new InfoGainSplitCriterion(minBranch)
    case gini: GiniSplitCriterionType            => new GiniSplitCriterion()
    case vr: VarianceReductionSplitCriterionType => new VarianceReductionSplitCriterion()
    case _                                       => new InfoGainSplitCriterion(minBranch)
  }
}
