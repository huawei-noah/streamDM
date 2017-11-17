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

package org.apache.spark.streamdm.clusterers.clusters

import org.apache.spark.streamdm.core._

/**
 * A MicroCluster contains the underlying structure for the Clustream processing
 * framework, and summarizes a number of instances belonging to the same
 * MicroCluster. It is composed of: 
 * - the number of instance
 * - the sum of instances
 * - the sum of squared instance
 * - the sum of timestamps
 * - the squared sum of timestamps.
 */
case class MicroCluster(val sum:Instance, val sqSum: Instance, 
  val timeSum: Long, val sqTimeSum: Double, val num: Int)
  extends Serializable {
 
  /**
   * Insert an instance into the microcluster.
   *
   * @param inst the Instance which needs a class predicted
   * @param time the timestamp of the Instance
   * @return the new MicroCluster
   */
  def insert(inst: Instance, time: Long): MicroCluster = sum match {
    case NullInstance() =>
      new MicroCluster(inst.map(x=>x),inst.map(x=>x*x), time,
        time.toDouble*time.toDouble, 1)
    case _ =>
      new MicroCluster(sum.add(inst), sqSum.add(inst.map(x=>x*x)),timeSum+time,
                       sqTimeSum+time.toDouble*time.toDouble, num+1)
  }
   
  /**
   * Merges two microclusters together.
   *
   * @param other the MicroCluster which gets added
   * @return the new MicroCluster
   */
  def merge(other: MicroCluster): MicroCluster =
    new MicroCluster(sum.add(other.sum), sqSum.add(other.sqSum),
      timeSum+other.timeSum, sqTimeSum+other.sqTimeSum, num+other.num)
   
  /**
   * Compute the centroid of a Microcluster.
   * 
   * @return an instance representing the centroid
   */ 
  def centroid: Instance =
    if(num>1)
      sum.map(x=>x/num.toDouble)
    else
      sum.map(x=>x)

  /**
   * Compute the RMSE of a microcluster.
   *
   * @return the RMSE of a microcluster
   */
  def rmse: Double = {
    if(num<=1)
      Double.MaxValue
    else {
      val centr = this.centroid
      math.sqrt((sqSum.add(centr.map{case x=> x*x}).add(sum.hadamard(centr).
        map(-2*_)).reduce(_+_))/num)
    }
  }

  /**
   * Compute the threshold timestamp of the microcluster.
   * 
   * @return the threshold timestamp
   */
  def threshold(m: Int): Double = {
    if(num<=1)
      Double.MaxValue
    else {
      val mu = timeSum / num
      if(num<2*m)
        mu
      else {
        val z = 2*m/num
        val sd = sqTimeSum/num - mu*mu
        mu+sd*math.sqrt(2)*MicroClusterUtils.inverr(2*z-1)
      }
    }
  }

  /**
   * Return the microcluster as a weighted Example, containing the centroid and
   * the number of elements.
   *
   * @return the output Example 
   */ 
  def toExample: Example =
    new Example(this.centroid, new NullInstance(), this.num)

  override def toString: String =
    "%s %d".format(centroid.toString, num)

}

/* Helper functions for MicroClusters. */
object MicroClusterUtils {
  /**
   * Compute the inverse error functions, for computing the timestamp threshold
   * in a MicroCluster.
   */
  def inverr(x:Double): Double = {
    val pi = 3.14159
    val a = 0.140012
    val sgn = if(x<0) -1 else 1
    val lg = math.log(1-x*x)
    val fr = 2/(pi*a)+lg/2
    sgn*math.sqrt(math.sqrt(fr*fr-lg/a)-fr)
  }
}
