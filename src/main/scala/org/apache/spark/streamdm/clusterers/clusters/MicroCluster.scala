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
 
  /* Insert an instance into the microcluster 
   *
   * @param inst the Instance which needs a class predicted
   * @param time the timestamp of the Instance
   * @return the new MicroCluster
   */
  def insert(inst: Instance, time: Long): MicroCluster =
    new MicroCluster(sum.add(inst), sqSum.add(inst.map(x=>x*x)), timeSum+time,
                     sqTimeSum+time.toDouble*time.toDouble, num+1)

   
  /* Merges two microclusters together 
   *
   * @param other the MicroCluster which gets added
   * @return the new MicroCluster
   */
  def merge(other: MicroCluster): MicroCluster =
    new MicroCluster(sum.add(other.sum), sqSum.add(other.sqSum),
      timeSum+other.timeSum, sqTimeSum+other.sqTimeSum, num+other.num)
   
  /* Compute the centroid of an Instance 
   *
   * @return an instance representing the centroid
   */ 
  def centroid: Instance =
    if(num>1)
      sum.map(x=>x/num.toDouble)
    else
      sum
}
