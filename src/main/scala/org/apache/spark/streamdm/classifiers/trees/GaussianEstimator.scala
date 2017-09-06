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

import scala.math.{ sqrt, Pi, pow, exp, max }
import org.apache.spark.streamdm.utils.Statistics

/**
 * Gaussian incremental estimator that uses incremental method, more resilient
 * to floating point imprecision.
 * For more info see Donald Knuth's "The Art of Computer Programming, Volume 2:
 * Seminumerical Algorithms", section 4.2.2.
 */

class GaussianEstimator(var weightSum: Double = 0.0, var mean: Double = 0.0,
                        var varianceSum: Double = 0.0) extends Serializable {
  val normal_constant: Double = sqrt(2 * Pi)
  var blockWeightSum: Double = 0.0
  var blockMean: Double = 0.0
  var blockVarianceSum: Double = 0.0

  def this(that: GaussianEstimator) {
    this(that.weightSum, that.mean, that.varianceSum)
  }
  /**
   * Observe the data and update the Gaussian estimator
   *
   * @param value value of a feature
   * @param weight weight of the Example
   */
  def observe(value: Double, weight: Double): Unit = {
    if (!value.isInfinite() && !value.isNaN() && weight > 0) {
      if (blockWeightSum == 0) {
        blockMean = value
        blockWeightSum = weight
      } else {
        blockWeightSum += weight
        val lastMean = blockMean
        blockMean += weight * (value - lastMean) / blockWeightSum
        blockVarianceSum += weight * (value - lastMean) * (value - blockMean)
      }
    }
  }
  /**
   * Merge current GaussianEstimator with another one.
   *
   * @param that the GaussianEstimator to be merged
   * @param trySplit flag indicating whether the Hoeffding Tree tries to split
   * @return the new GaussianEstimator
   */
  def merge(that: GaussianEstimator, trySplit: Boolean): GaussianEstimator = {
    if (!trySplit) {
      //add to block variables
      if (this.blockWeightSum == 0) {
        blockWeightSum = that.blockWeightSum
        blockMean = that.blockMean
        blockVarianceSum = that.blockVarianceSum
      } else {
        val newBlockWeightSum = blockWeightSum + that.blockWeightSum
        val newBlockMean = (this.blockMean * blockWeightSum + that.blockMean *
          that.blockWeightSum) / newBlockWeightSum
        val newBlockVarianceSum = this.blockVarianceSum + that.blockVarianceSum +
          pow(this.blockMean - that.blockMean, 2) *
          this.blockWeightSum * that.blockWeightSum / (this.blockWeightSum + that.blockWeightSum)
        blockWeightSum = newBlockWeightSum
        blockMean = newBlockMean
        blockVarianceSum = newBlockVarianceSum
      }
    } else {
      //add to the total variables
      if (this.weightSum == 0) {
        weightSum = that.blockWeightSum
        mean = that.blockMean
        varianceSum = that.blockVarianceSum
      } else {
        val newWeightSum = weightSum + that.blockWeightSum
        val newMean = (this.mean * weightSum + that.blockMean * that.blockWeightSum) / 
                        newWeightSum
        val newVarianceSum = this.varianceSum + that.blockVarianceSum + pow(this.mean -
                              that.blockMean, 2) * this.weightSum * that.blockWeightSum /
                              (this.weightSum + that.blockWeightSum)
        weightSum = newWeightSum
        mean = newMean
        varianceSum = newVarianceSum
      }
    }
    this
  }

  /**
   * Returns the total weight
   *
   * @return the total weight
   */
  def totalWeight(): Double = {
    weightSum
  }
  /**
   * Returns the mean value
   *
   * @return the mean value
   */
  def getMean(): Double = {
    mean
  }
  /**
   * Returns the standard deviation
   *
   * @return the standard deviation
   */
  def stdDev(): Double = {
    sqrt(variance())
  }

  /**
   * Returns the variance
   *
   * @return the variance
   */
  def variance(): Double = {
    if (weightSum <= 1.0) 0
    else varianceSum / (weightSum - 1)
  }

  /**
   * Returns the cumulative probability of the input value in the current
   * distribution.
   *
   * @param value the value
   * @return the cumulative probability
   */

  def probabilityDensity(value: Double): Double = {
    if (weightSum == 0) 0.0
    else {
      val stddev = stdDev()
      if (stddev > 0) {
        val diff = value - mean
        exp(-pow(diff / stddev, 2) / 2) / (normal_constant * stddev)
      } else {
        if (value == mean) 1.0 else 0
      }
    }
  }

  /**
   * Returns an array of weights  which have the sum less than, equal to, and
   * greater than the split value. 
   *
   * @param splitValue the value of the split 
   * @return the resulting Array of values
   */
  def tripleWeights(splitValue: Double): Array[Double] = {
    //equal weights sum
    val eqWeight = probabilityDensity(splitValue) * weightSum
    //less than weights sum
    val lsWeight = {
      if (stdDev() > 0) {
        Statistics.normalProbability((splitValue - getMean()) / stdDev()) * weightSum - eqWeight
      } else {
        if (splitValue < getMean()) weightSum - eqWeight
        else 0.0
      }
    }
    //greater than weights sum
    val gtWeight = max(0, weightSum - eqWeight - lsWeight)
    Array[Double](lsWeight, eqWeight, gtWeight)
  }
}
