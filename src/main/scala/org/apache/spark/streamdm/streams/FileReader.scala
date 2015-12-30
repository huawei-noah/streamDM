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

import java.io._
import scala.io._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.rdd.RDD

import com.github.javacliparser.{ IntOption, FloatOption, StringOption, FileOption }

import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.core.specification._
import org.apache.spark.streamdm.streams.generators.Generator
//import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * FileReader is used to read data from one file of full data to simulate a stream data.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Chunk size (<b>-k</b>)
 *  <li> Slide duration in milliseconds (<b>-d</b>)
 *  <li> Type of the instance to use, it should be "dense" or "sparse" (<b>-t</b>)
 *  <li> Data File Name (<b>-f</b>)
 *  <li> Data Header Format,uses weka's arff as default.(<b>-h</b>)
 * </ul>
 */

class FileReader extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 10000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val instanceOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")

  val fileNameOption: StringOption = new StringOption("fileName", 'f',
    "File Name", "./sampleData")

  val dataHeadTypeOption: StringOption = new StringOption("dataHeadType", 'h',
    "Data Head Format", "arff")
  var fileName: String = null
  var headFileName: String = null
  var isInited: Boolean = false
  var hasHeadFile: Boolean = false
  var lines: Iterator[String] = null

  def init() {
    if (!isInited) {
      try {
        fileName = fileNameOption.getValue
        val file = new File(fileName)
        if (!file.exists()) {
          println("file does not exists, input a new file name")
          exit()
        }
        headFileName = fileNameOption.getValue() + "." + dataHeadTypeOption.getValue + ".head"
        val hfile: File = new File(headFileName)
        if (hfile.exists()) {
          // has a head file 
          hasHeadFile = true
        }
        isInited = true
      }
    }
  }

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */
  override def getExampleSpecification(): ExampleSpecification = {
    init()
    SpecificationHead.getSpecification(if (hasHeadFile) headFileName else fileName, dataHeadTypeOption.getValue)
  }
  /**
   * Get one Exmaple from file
   *
   * @return an Exmaple
   */
  def getExampleFromFile(): Example = {
    var exp: Example = null
    if (lines == null) {
      lines = Source.fromFile(fileName).getLines()
    }
    // if reach the end of file, will go to the head again
    if (!lines.hasNext) {
      lines = Source.fromFile(fileName).getLines()
    }
    var line = lines.next()
    while (!hasHeadFile && (line.startsWith(" ") || line.startsWith("%") || line.startsWith("@"))) {
      line = lines.next()
    }
    if (!hasHeadFile) {
      //if it doesn't have a head file, it means the data in other format
      line = dataHeadTypeOption.getValue() match {
        case "arff" => {
          val index: Int = line.lastIndexOf(",")
          line.substring(index + 1) + " " + line.substring(0, index)
        }
        case "csv" => {
          // we assume the first column is the label
          val index: Int = line.indexOf(",")
          line.substring(0, index) + " " + line.substring(index + 1)
        }
        // other formats are not supported yet, may throw exception
        case _ => line
      }
    }
    exp = Example.parse(line, instanceOption.getValue, "dense")
    exp
  }

  /**
   * Obtains a stream of examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  override def getExamples(ssc: StreamingContext): DStream[Example] = {
    init()
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
    }
  }
}