package org.apache.spark.streamdm.classifiers.trees

import scala.math.Ordered
// Feature split suggestion
class FeatureSplit(val conditionalTest: ConditionalTest, val merit: Double, val result: Array[Array[Double]]) extends Ordered[FeatureSplit] {
  override def compare(that: FeatureSplit): Int = {
    if (this.merit < that.merit) -1
    else if (this.merit > that.merit) 1
    else 0
  }
  def numSplit: Int = result.length

  def distributionFromSplit(splitIndex: Int): Array[Double] = result(splitIndex)

}