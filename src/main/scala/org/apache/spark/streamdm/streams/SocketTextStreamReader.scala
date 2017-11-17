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

import com.github.javacliparser.{ StringOption, IntOption }
import org.apache.spark.streamdm.core.specification._
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * Stream reader that gets instances from a socket stream.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Socket host (<b>-h</b>)
 *  <li> Socket port (<b>-p</b>)
 *  <li> Instance type (<b>-t</b>), either <i>dense</i> or <i>sparse</i>
 * </ul>
 */
class SocketTextStreamReader extends StreamReader {

  val portOption: IntOption = new IntOption("port", 'p',
    "Socket port", 9999, 0, Integer.MAX_VALUE)

  val hostOption: StringOption = new StringOption("host", 'h', "Host",
    "localhost")

  val instanceOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")

  /**
   * Obtains a stream of examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  def getExamples(ssc: StreamingContext): DStream[Example] = {
    //stream is a localhost socket stream
    val text = ssc.socketTextStream(hostOption.getValue, portOption.getValue)
    //transform stream into stream of instances
    //instances come as whitespace delimited lines, where the first item is the
    //instance of the label(s) and the second is the instance of the features
    text.map(x => Example.parse(x, instanceOption.getValue, "dense"))
  }

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */
  override def getExampleSpecification(): ExampleSpecification = {

    //Prepare specification of class attributes
    val outputIS = new InstanceSpecification()
    val classFeature = new NominalFeatureSpecification(Array("+", "-"))
    outputIS.addFeatureSpecification(0, "class", classFeature)

    new ExampleSpecification(new InstanceSpecification(),
      outputIS)
  }
}
