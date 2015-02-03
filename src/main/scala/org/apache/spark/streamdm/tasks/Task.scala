package org.apache.spark.streamdm.tasks

import java.io.Serializable

import com.github.javacliparser.Configurable
import org.apache.spark.streaming.StreamingContext

abstract class Task extends Configurable with Serializable{
  def run(ssc:StreamingContext): Unit
}