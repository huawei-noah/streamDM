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

package org.apache.spark.streamdm.clusterers

import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * A Clusterer trait defines the needed operations for any implemented
 * clustering algorithm. It provides methods for clustering and for returning
 * the computed cluster.
 */
trait Clusterer extends Learner  with Serializable {

  /* Get the currently computed clusters
   * @return an Array of Examples representing the clusters
   */
  def getClusters: Array[Example]

  /* Assigns examples to clusters, given the current Clusters data structure. 
   *
   * @param input the DStream of Examples to be assigned a cluster
   * @return a DStream of tuples containing the original Example and the
   * assigned cluster.
   */
  def assign(input: DStream[Example]): DStream[(Example,Double)]
}
