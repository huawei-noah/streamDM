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

package org.apache.spark.streamdm.streams.generators

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{ Duration, Time, StreamingContext }
import org.apache.spark.streaming.dstream.{ InputDStream, DStream }
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader

abstract class Generator extends StreamReader {

  /**
   * returns chunk size
   */
  def getChunkSize(): Int

  /**
   * returns slide duration
   */
  def getslideDuration(): Int

  /**
   * initializes the generator
   */
  def init(): Unit
  // call initialization
  
   var inited:Boolean = false
  
  /**
   * generates a random example.
   *
   * @return a random example
   */

  def getExample(): Example

  /**
   * Obtains a stream of random examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  def getExamples(ssc: StreamingContext): DStream[Example] = {
    init()
    new InputDStream[Example](ssc) {

      override def start(): Unit = {}

      override def stop(): Unit = {}

      override def compute(validTime: Time): Option[RDD[Example]] = {
        Some(ssc.sparkContext.parallelize(getExamples()))
      }
      override def slideDuration = {
        new Duration(getslideDuration)
      }
    }
  }

  /**
   * obtains an array of examples
   *
   * @param length size of the array; default chunkSizeOption.getValue
   * @return an array of Examples
   */
  def getExamples(length: Int = getChunkSize()): Array[Example] = {
    init()
    Array.fill[Example](length)(getExample())
  }
}