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

import org.apache.spark.streamdm.streams.generators.Generator
import com.github.javacliparser.{ IntOption, FloatOption, StringOption, FileOption }
import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.core.specification._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.io._
import java.io._
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * FileReader is used to read data from one file of full data to simulate a stream data.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Chunk size (<b>-k</b>)
 *  <li> Slide duration in milliseconds (<b>-d</b>)
 *  <li> Type of the instance to use, it should be "dense" or "sparse" (<b>-t</b>)
 *  <li> File to use (<b>-f</b>)
 * </ul>
 */

class FileReader extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 10000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val instanceOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")

  val fileNameOption: FileOption = new FileOption("fileName", 'f',
    "File to use", null, "txt", false)

  def read(ssc: StreamingContext): DStream[Example] = {
    val file: File = new File(fileNameOption.getValue)
    if (!file.exists()) {
      println("file does not exists, input a new file name")
      exit()
    } else {
      var lines = Source.fromFile(fileNameOption.getValue).getLines()
      new InputDStream[Example](ssc) {
        override def start(): Unit = {}

        override def stop(): Unit = {}

        override def compute(validTime: Time): Option[RDD[Example]] = {
          val examples: Array[Example] = Array.fill[Example](chunkSizeOption.getValue)(getExampleFromFile())
          Some(ssc.sparkContext.parallelize(examples))
        }

        override def slideDuration = {
          new Duration(slideDurationOption.getValue)
        }

        def getExampleFromFile(): Example = {
          var exp: Example = null
          if (lines.hasNext) {
            exp = Example.parse(lines.next(), instanceOption.getValue, "dense")
          } else {
            lines = Source.fromFile(fileNameOption.getValue).getLines()
            exp = Example.parse(lines.next(), instanceOption.getValue, "dense")
          }
          exp
        }
      }
    }
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
    outputIS.setFeatureSpecification(0, classFeature)
    outputIS.setName(0, "class")

    new ExampleSpecification(new InstanceSpecification(),
      outputIS)
  }

  /**
   * Obtains a stream of examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  override def getExamples(ssc: StreamingContext): DStream[Example] = {
    read(ssc)
  }
}