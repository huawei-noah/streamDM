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
import com.github.javacliparser.Configurable

/**
 * Abstract class that defines the output from a DStream of Strings.
 */
abstract class StreamWriter extends Configurable {
  /**
   * Process the output.
   * 
   * @param stream a DStream of Strings for which the output is processed 
   */
  def output(stream: DStream[String]): Unit

  /**
    * Writes a single line on the output. Useful for writing one-time text, such as headers.
    *
    * @param text
    */
  def output(text: String): Unit
}
