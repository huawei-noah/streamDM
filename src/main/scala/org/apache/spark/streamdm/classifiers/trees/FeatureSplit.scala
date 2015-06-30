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

import scala.math.Ordered

/**
 * Class for recording a split suggestion.
 */
class FeatureSplit(val conditionalTest: ConditionalTest, val merit: Double, 
  val result: Array[Array[Double]]) extends Ordered[FeatureSplit] {

  /** Compares two FeatureSplit objects.
   * @param that the comparison feature 
   * @return comparison result
   */
  override def compare(that: FeatureSplit): Int = {
    if (this.merit < that.merit) -1
    else if (this.merit > that.merit) 1
    else 0
  }

  /**
   * Returns the number of the split.
   * @return the number of the split
   */
  def numSplit(): Int = result.length

  /**
   * Returns the distribution of the split index.
   *
   * @param splitIndex the split index
   * @return an Array containing the distribution   
   */
  def distributionFromSplit(splitIndex: Int): Array[Double] = result(splitIndex)

  override def toString(): String = "FeatureSplit, merit=" + merit + ", " + conditionalTest

}
