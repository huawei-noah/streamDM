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
 * A Clusters trait defines the needed operations for maintaining a clustering
 * data structure. It mainly provides a method to update the data structure
 * based on an Instance.
 */
trait Clusters extends Model {

  type T <: Clusters

  /**
   * Update the clustering data structure, depending on the Example given.
   *
   * @param inst the Example based on which the Model is updated
   * @return the updated Clusters object
   */
  override def update(change: Example): T
}
