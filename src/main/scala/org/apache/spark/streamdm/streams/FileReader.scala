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

import java.io.File
import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }

import com.github.javacliparser.{ IntOption, FloatOption, StringOption, FileOption }

import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.core.specification._
import org.apache.spark.streamdm.streams.generators.Generator
import org.apache.spark.streamdm.core.specification._

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

class FileReader extends StreamReader with Logging {

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

  val headParser = new SpecificationParser
  var fileName: String = null
  var headFileName: String = null
  var isInited: Boolean = false
  var hasHeadFile: Boolean = false
  var lines: Iterator[String] = null
  var spec: ExampleSpecification = null

  def init() {
    if (!isInited) {
      fileName = fileNameOption.getValue
      val file = new File(fileName)
      if (!file.exists()) {
        logError("file does not exists, input a new file name")
        exit()
      }
      headFileName = fileNameOption.getValue() + "." +
        dataHeadTypeOption.getValue + ".head"
      val hfile: File = new File(headFileName)
      if (hfile.exists()) {
        // has a head file 
        hasHeadFile = true
      }
      spec = headParser.getSpecification(
        if (hasHeadFile) headFileName else fileName, dataHeadTypeOption.getValue)
      //      logInfo("IN:" + spec.in.size() + ",OUT:" + spec.out.size())
      //      logInfo(headParser.getHead(spec))
      //      for (index <- 0 until spec.in.size()) {
      //        logInfo("" + spec.inputFeatureSpecification(index))
      //      }
      //      logInfo("CCC" + spec.out(0) + spec.out(0).range())
      for (index <- 0 until spec.out(0).range()) {
        logInfo(spec.out(0).asInstanceOf[NominalFeatureSpecification](index))
      }

      isInited = true
    }
  }

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */
  override def getExampleSpecification(): ExampleSpecification = {
    init()
    spec
  }

  /**
   * Get one Exmaple from file
   *
   * @return an Exmaple
   */
  def getExampleFromFile(): Example = {
    var exp: Example = null
    if (lines == null || !lines.hasNext) {
      lines = Source.fromFile(fileName).getLines()
    }
    // if reach the end of file, will go to the head again
    if (!lines.hasNext) {
      lines = Source.fromFile(fileName).getLines()
    }
    var line = lines.next()
    while (!hasHeadFile && (line == "" || line.startsWith(" ") ||
      line.startsWith("%") || line.startsWith("@"))) {
      //logInfo(line)
      if (!lines.hasNext)
        lines = Source.fromFile(fileName).getLines()
      line = lines.next()
    }
    if (!hasHeadFile) {
      //logInfo("UUUU" + line)
      if ("arff".equalsIgnoreCase(dataHeadTypeOption.getValue())) {
        exp = ExampleParser.fromArff(line, spec)
      } else {
        if ("csv".equalsIgnoreCase(dataHeadTypeOption.getValue())) {
          //for the csv format, we assume the first is the classification
          val index: Int = line.indexOf(",")
          line = line.substring(0, index) + " " + line.substring(index + 1).trim()
          exp = Example.parse(line, instanceOption.getValue, "dense")
        }
      }
    } else {
      exp = Example.parse(line, instanceOption.getValue, "dense")
    }
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