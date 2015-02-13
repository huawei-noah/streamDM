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

import com.github.javacliparser.{StringOption, IntOption}
import org.apache.spark.streamdm.core.DenseSingleLabelInstance
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streamdm.core.Instance

/**
 * Stream reader that gets instances from a socket stream
 */
class SocketTextStreamReader extends StreamReader{

  val portOption: IntOption = new IntOption("port", 'p',
    "Socket port", 9999, 0, Integer.MAX_VALUE)

  val hostOption: StringOption = new StringOption("host",
    'h',"Host", "localhost")

  def getInstances(ssc:StreamingContext): DStream[Example] = {
    //stream is a localhost socket stream
    val text = ssc.socketTextStream(hostOption.getValue, portOption.getValue)
    //transform stream into stream of instances
    //instances come as tab delimited lines, where the first item is the label,
    //and the rest of the items are the values of the features
    text.map(x => new Example(DenseSingleLabelInstance.parse(x)))
  }
}
