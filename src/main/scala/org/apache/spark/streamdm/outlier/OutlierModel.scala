/*
 * Copyright (C) 2019 Télécom ParisTech LTCI lab.
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
package org.apache.spark.streamdm.outlier

import org.apache.spark.streamdm.core.{Example, Model}

/**
  * The OutlierModel trait defines the needed operations for updating an outlier/anomaly
  * detection model using a single Example.
  */
trait OutlierModel extends Model {

  type T <: OutlierModel

  /**
    * Update the outlier model structure, depending on the Example given.
    *
    * @param example the Example based on which the Model is updated
    * @return the updated OutlierModel object
    */
  override def update(example: Example): T
}
