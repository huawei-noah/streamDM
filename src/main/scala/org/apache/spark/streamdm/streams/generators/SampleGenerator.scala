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