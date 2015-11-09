package org.apache.spark.streamdm.streams.generators

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{ Duration, Time, StreamingContext }
import org.apache.spark.streaming.dstream.{ InputDStream, DStream }
import com.github.javacliparser.IntOption
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader

abstract class Generator extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'c',
    "Chunk Size", 1000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)
  def init(): Unit
  def getExample(): Example

  /**
   * Obtains a stream of random examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
  def getExamples(ssc: StreamingContext): DStream[Example] = {
    new InputDStream[Example](ssc) {

      override def start(): Unit = {}

      override def stop(): Unit = {}

      override def compute(validTime: Time): Option[RDD[Example]] = {
        init()
        val examples: Array[Example] = Array.fill[Example](chunkSizeOption.
          getValue)(getExample)
        Some(ssc.sparkContext.parallelize(examples))
      }

      override def slideDuration = {
        new Duration(slideDurationOption.getValue)
      }
    }
  }
}