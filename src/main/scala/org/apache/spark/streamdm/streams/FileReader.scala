/**
 *
 */
package org.apache.spark.streamdm.streams

import org.apache.spark.streamdm.streams.generators.Generator
import com.github.javacliparser.{ IntOption, FloatOption, StringOption, FileOption }
import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.io._
import java.io._

class FileReader extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 10000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'w',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val instanceOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")

  val fileNameOption: FileOption = new FileOption("fileName", 'f',
    "file to be loaded", null, "txt", false)

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
  
  def getExampleSpecification(): ExampleSpecification = {null}
  
  override def getExamples(ssc: StreamingContext): DStream[Example] = {
    read(ssc)
  }
}