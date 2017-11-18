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

import org.apache.spark.streaming.dstream.DStream

/**
 * Stream writer that outputs the stream to the standard output.
 */
class PrintStreamWriter extends StreamWriter{

  /**
   * Process the output.
   * <p>
   * In the PrintStreamWriter, each String in the stream is sent to stdout.
   * 
   * @param stream a DStream of Strings for which the output is processed 
   */
  def output(stream: DStream[String]) = {
    stream.foreachRDD(rdd => {
      rdd.collect().foreach(x => println(x))
    })
  }

  /**
    * Prints a single line of text to the output only once.
    * @param text
    */
  override def output(text: String) = println(text)
}
