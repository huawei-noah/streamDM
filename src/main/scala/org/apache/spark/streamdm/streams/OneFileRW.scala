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

package org.apache.spark.streamdm.streams

/**
 * trait OneFileRW provides functions to save examples to one file and generate DStream from one file.
 */

trait OneFileRW {

  /**
   * Saves Example array to a local file
   *
   * @param exampls array of Example
   * @param fileName a file name
   * @return Unit
   */
  def writeToLocal(exampls: Array[Example], fileName: String): Unit

  /**
   * Saves Example array to a local file
   *
   * @param exampls array of Example
   * @param fileName a HDFS file name
   * @return Unit
   */
  def writeToHDFS(exampls: Array[Example], fileName: String): Unit

  /**
   * Creates Dstream of Example from a local file
   *
   * @param exampls array of Example
   * @param fileName a file name
   * @param ssc a Spark Streaming context
   * @chunkSize chunk size of a RDD
   * @return a Dstream of Example
   */
  def readFromLocal(fileName: String, ssc: StreamingContext, chunkSize: Int): DStream[Example]

  /**
   * Createsave  Dstream of Example from a HDFS file
   *
   * @param exampls array of Example
   * @param fileName a HDFS file name
   * @param ssc a Spark Streaming context
   * @chunkSize chunk size of a RDD
   * @return a Dstream of Example
   */
  def readFromHDFS(fileName: String, ssc: StreamingContext, chunkSize: Int): DStream[Example]
}