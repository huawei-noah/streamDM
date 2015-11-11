package org.apache.spark.streamdm.streams.generators

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{ Duration, Time, StreamingContext }
import org.apache.spark.streaming.dstream.{ InputDStream, DStream }
import com.github.javacliparser.IntOption
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader

abstract class Generator extends StreamReader {

  /**
   * returns chunk size
   */
  def getChunkSize(): Int

  /**
   * returns slide duration
   */
  def getslideDuration(): Int

  /**
   * initializes the generator
   */
  def init(): Unit
  // call initialization
  init()
  /**
   * generates a random example.
   *
   * @return a random example
   */

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
        Some(ssc.sparkContext.parallelize(getExamples()))
      }
      override def slideDuration = {
        new Duration(getslideDuration)
      }
    }
  }

  /**
   * obtains an array of examples
   *
   * @param length size of the array; default chunkSizeOption.getValue
   * @return an array of Examples
   */
  def getExamples(length: Int = getChunkSize()): Array[Example] = {
    Array.fill[Example](length)(getExample())
  }
}