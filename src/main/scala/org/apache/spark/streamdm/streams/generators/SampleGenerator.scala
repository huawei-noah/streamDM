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

import com.github.javacliparser.{ IntOption, FloatOption }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader
import org.apache.spark.streaming.{ Duration, Time, StreamingContext }
import org.apache.spark.streaming.dstream.{ InputDStream, DStream }

class SampleGenerator extends Generator {
  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'c',
    "Chunk Size", 1000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)
  /**
   * returns chunk size
   */
  override def getChunkSize(): Int = {
    chunkSizeOption.getValue
  }

  /**
   * returns slide duration
   */
  override def getslideDuration(): Int = {
    slideDurationOption.getValue
  }

  /**
   * initializes the generator
   */
  override def init(): Unit = {}

  /**
   * generates a random example.
   *
   * @return a random example
   */

  override def getExample(): Example = { null }

  /**
   * Obtains a stream of random examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */
  override def getExampleSpecification(): ExampleSpecification = null
}