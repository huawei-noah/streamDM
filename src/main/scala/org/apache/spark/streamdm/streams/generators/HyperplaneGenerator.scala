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

import com.github.javacliparser.IntOption
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader
import org.apache.spark.streaming.{Duration, Time, StreamingContext}
import org.apache.spark.streaming.dstream.{InputDStream, DStream}

import scala.util.Random


/**
 * Stream generator for generating data from a hyperplane.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Chunk size (<b>-c</b>)
 *  <li> Slide duration in milliseconds (<b>-d</b>)
 *  <li> Number of features (<b>-f</b>)
 * </ul>
 */

class HyperplaneGenerator extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'c',
    "Chunk Size", 1000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  /**
   * Obtains a stream of random examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  def getExamples(ssc:StreamingContext): DStream[Example] = {
    new InputDStream[Example](ssc){

      override def start(): Unit = {}

      override def stop(): Unit = {}

      override def compute(validTime: Time): Option[RDD[Example]] = {
        val examples:Array[Example] = Array.fill[Example](chunkSizeOption.
              getValue)(getExample)
          Some(ssc.sparkContext.parallelize(examples))
      }

      override def slideDuration = {
        new Duration(slideDurationOption.getValue)
      }

      def getExample(): Example = {
        val inputInstance = new DenseInstance(Array.fill[Double](
            numFeaturesOption.getValue)(5.0 * getRandomNumber()))
        val noiseInstance = new DenseInstance(Array.fill[Double](
            numFeaturesOption.getValue)(getNoise()))
        new Example(inputInstance.add(noiseInstance), new DenseInstance(
            Array.fill[Double](1)(label(inputInstance))))
      }

      def getRandomNumber():Double = 2.0 * Random.nextDouble() - 1.0 

      def getNoise():Double = 0.5 * Random.nextGaussian()

      val weight = new DenseInstance(Array.fill[Double](
          numFeaturesOption.getValue)(getRandomNumber()))

      val bias:Double = getRandomNumber()

      def label(inputInstance: Instance):Double = {
        val sum = weight.dot(inputInstance)
        if (sum > bias) 1
          else 0
      }
    }
  }

  def init(): Unit = {}

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the examples
   */
  def getExampleSpecification(): ExampleSpecification = {

    //Prepare specification of class attributes
    val outputIS = new InstanceSpecification()
    val classFeature = new NominalFeatureSpecification(Array("+","-"))
    outputIS.setFeatureSpecification(0, classFeature)
    outputIS.setName(0, "class")

    //Prepare specification of input attributes
    val inputIS = new InstanceSpecification()
    for (i <- 1 to numFeaturesOption.getValue) inputIS.setName(i, "Feature" + i)

    new ExampleSpecification(inputIS, outputIS)
  }

}
