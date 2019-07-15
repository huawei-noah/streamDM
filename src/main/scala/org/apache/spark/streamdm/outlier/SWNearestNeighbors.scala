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

import com.github.javacliparser.{FlagOption, IntOption}
import org.apache.spark.internal.Logging
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.core.specification.ExampleSpecification
import org.apache.spark.streaming.dstream.DStream

import scala.collection.mutable.Queue

/**
  * The Sliding Window Nearest Neighbors (SWNearestNeighbors) is a global point anomaly detection technique.
  * The intuition behind this method is that data points 'far' from the majority of the data points may be
  * considered outliers (i.e., anomalies).
  * The implementation uses a sliding window per cluster node.
  * See [[org.apache.spark.streamdm.outlier.SWNearestNeighborsModel]].
  */
class SWNearestNeighbors extends Outlier with Logging {

  type T = SWNearestNeighborsModel

  val windowMaximumSizeOption: IntOption = new IntOption("windowMaximumSize", 'n',
    "maximum size of the sliding window", 1000, 0, Int.MaxValue)

  val debugOption: FlagOption = new FlagOption("debug", 'd',
    "write debug information to the log as [INFO]")

  var model: T = null

  override def init(exampleSpecification: ExampleSpecification): Unit = {
    this.model = new SWNearestNeighborsModel(windowMaximumSizeOption.getValue(), debugOption.isSet())
  }

  /**
    * This is an unsupervised outlier detection method, therefore there is no training
    * @param input a stream of Examples
    */
  override def train(input: DStream[Example]): Unit = {
  }

  override def getModel: T = this.model

  /**
    * Calculates the outlierness score of the examples from the stream and store them.
    * @param input a data stream of examples
    * @return the 'outlierness' score
    */
  override def outlierness(input: DStream[Example]): DStream[(Example, Double)] = {
    val inputDStreamCached = input.cache()

    inputDStreamCached.map(e => {
      val output = (e, this.model.outlierness(e))
      this.model.update(e)
      output
    })
  }
}

class SWNearestNeighborsModel(
                               val windowMaximumSize: Int,
                               val debug: Boolean
                             ) extends OutlierModel with Logging {

  var window = new Queue[Example]()

  type T = SWNearestNeighborsModel

  /**
    * Update the sliding window with the new example
    * @param example the example added to the window
    * @return the updated SWNearestNeighborsModel (one example added to its window).
    */
  override def update(example: Example): T = {
    // If maximum size has been reached, remove the oldest instance.
    if(window.size >= windowMaximumSize)
      window.dequeue()
    window.enqueue(example)
    this
  }

  /**
    * Calculates the global 'outlierness' score of an example.
    * This function uses the build-in function distanceTo (Euclidean distance) from Instance.scala.
    *
    * @param example
    * @return The 'outlierness' score of example
    */
  def outlierness(example: Example): Double = {
    val distances = window.map(p => p.in.distanceTo(example.in))

    if(!distances.isEmpty) {
      val aggDistance = distances.reduce((d1, d2) => (d1 + d2)) / distances.size

      if(debug)
        logInfo("outlierness, %f, {%s}, %s, %d".format(aggDistance,
          example.in.getFeatureIndexArray().map(ins => ins._1).mkString(";"),
          example.out.getFeatureIndexArray().map(ins => ins._1).mkString(" "),
          distances.size))

      aggDistance
    } else {
      0.0
    }
  }
}
