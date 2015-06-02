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

import scala.math.{ max }

import org.apache.spark.streamdm.util.Util

/**
 * trait SplitCriterionType and the case classes
 */

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
 * class InfoGainSplitCriterion for computing splitting criteria using Information Gain
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
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
  override def rangeMerit(pre: Array[Double]): Double = Util.log2(max(pre.length, 2))

  /**
   * computes the entropy of an array
   */
  def entropy(pre: Array[Double]): Double = {
    if (pre == null || pre.sum <= 0 || hasNegative(pre)) 0.0
    Util.log2(pre.sum) - pre.filter(_ > 0).map(x => x * Util.log2(x)).sum / pre.sum
  }

  /**
   * computes the entropy of an matrix
   */
  def entropy(post: Array[Array[Double]]): Double = {
    if (post == null || post.length == 0 || post(0).length == 0) 0
    else {
      post.map { row => (row.sum * entropy(row)) }.sum / post.map(_.sum).sum
    }

  }

  /**
   * number of subsets which greater than minFrac
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
   * check whether a array has a negative value
   */
  private[trees] def hasNegative(pre: Array[Double]): Boolean = pre.filter(x => x < 0).length > 0

}

/**
 * class GiniSplitCriterion for computing splitting criteria using Gini
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
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

  private[trees] def computeGini(dist: Array[Double], sum: Double): Double =
    1.0 - dist.map { x => x * x / sum / sum }.sum

}

/**
 * class VarianceReductionSplitCriterion for computing splitting criteria
 * using variance reduction
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on decision trees and decision stumps.
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
   * computes standard deviation
   */
  private[trees] def computeSD(pre: Array[Double]): Double = {
    val n = pre(0).toInt
    val sum = pre(1)
    val sumSq = pre(2)
    (sumSq - ((sum * sum) / n)) / n
  }
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