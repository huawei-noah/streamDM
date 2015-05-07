package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ sqrt, Pi, pow, exp }

/**
 * Gaussian incremental estimator that uses incremental method that is more resistant to floating point imprecision.
 * for more info see Donald Knuth's "The Art of Computer Programming, Volume 2: Seminumerical Algorithms", section 4.2.2.
 */

class GaussianEstimator(var weightSum: Double = 0.0, var mean: Double = 0.0,
                        var varianceSum: Double = 0.0) extends Serializable {
  val normal_constant: Double = sqrt(2 * Pi)

  def observe(value: Double, weight: Double): Unit = {
    if (!value.isInfinite() && !value.isNaN() && weight > 0) {
      if (weightSum == 0) {
        mean = value
        weightSum = weight
      } else {
        weightSum += weight
        val lastMean = mean
        mean += weight * (value - lastMean) / weightSum
        varianceSum += weight * (value - lastMean) * (value - mean)
      }
    }
  }

  def merge(that: GaussianEstimator): GaussianEstimator = {
    if (this.weightSum == 0 || that.weightSum == 0) {
      if (this.weightSum == 0) {
        new GaussianEstimator(that.weightSum, that.mean, that.varianceSum)
      } else {
        new GaussianEstimator(weightSum, mean, varianceSum)
      }
    } else {
      val newWeightSum = weightSum + that.weightSum
      val newMean = (this.mean * weightSum + that.mean * that.weightSum) / newWeightSum
      val newVarianceSum = this.varianceSum + that.varianceSum + pow(this.mean - that.mean, 2) *
        this.weightSum * that.weightSum / (this.weightSum + that.weightSum)
      new GaussianEstimator(newWeightSum, newMean, newVarianceSum)
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
}