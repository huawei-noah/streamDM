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
import org.apache.spark.streamdm.util.Statistics

/**
 * Gaussian incremental estimator that uses incremental method that is more resistant to floating point imprecision.
 * for more info see Donald Knuth's "The Art of Computer Programming, Volume 2: Seminumerical Algorithms", section 4.2.2.
 */

class GaussianEstimator(val weightSum: Double = 0.0, val mean: Double = 0.0,
                        val varianceSum: Double = 0.0) extends Serializable {
  val normal_constant: Double = sqrt(2 * Pi)
  var blockWeightSum: Double = 0.0
  var blockMean: Double = 0.0
  var blockVarianceSum: Double = 0.0

  def this(that: GaussianEstimator) {
    this(that.weightSum, that.mean, that.varianceSum)
  }
  /*
   * observe the data and update the gaussian estimator
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
  /*
   * merge the two GaussianEstimator
   */
  def merge(that: GaussianEstimator, trySplit: Boolean): GaussianEstimator = {
    if (!trySplit) {
      if (this.weightSum == 0)
        new GaussianEstimator(that.blockWeightSum, that.blockMean, that.blockVarianceSum)
      else {
        val newWeightSum = weightSum + that.blockWeightSum
        val newMean = (this.mean * weightSum + that.blockMean * that.blockWeightSum) / newWeightSum
        val newVarianceSum = this.varianceSum + that.blockVarianceSum + pow(this.mean - that.blockMean, 2) *
          this.weightSum * that.blockWeightSum / (this.weightSum + that.blockWeightSum)
        new GaussianEstimator(newWeightSum, newMean, newVarianceSum)
      }
    } else {
      if (this.weightSum == 0)
        new GaussianEstimator(that.weightSum, that.mean, that.varianceSum)
      else {
        val newWeightSum = weightSum + that.weightSum
        val newMean = (this.mean * weightSum + that.mean * that.weightSum) / newWeightSum
        val newVarianceSum = this.varianceSum + that.varianceSum + pow(this.mean - that.mean, 2) *
          this.weightSum * that.weightSum / (this.weightSum + that.weightSum)
        new GaussianEstimator(newWeightSum, newMean, newVarianceSum)
      }
    }
  }

  def totalWeight(): Double = {
    weightSum
  }

  def getMean(): Double = {
    mean
  }

  def stdDev(): Double = {
    sqrt(variance())
  }

  def variance(): Double = {
    if (weightSum <= 1.0) 0
    else varianceSum / (weightSum - 1)
  }

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

  /*
   * return an array with lessthan, equal and greaterthan of split value
   */
  def tripleWeights(splitValue: Double): Array[Double] = {
    val eqWeight = probabilityDensity(splitValue) * weightSum
    val lsWeight = {
      if (stdDev() > 0) {
        Statistics.normalProbability((splitValue - getMean()) / stdDev())
      } else {
        if (splitValue < getMean()) weightSum - eqWeight
        else 0.0
      }
    }
    val gtWeight = max(0, weightSum - eqWeight - lsWeight)
    Array[Double](lsWeight, eqWeight, gtWeight)
  }
}