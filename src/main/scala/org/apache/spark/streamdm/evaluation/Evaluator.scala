package org.apache.spark.streamdm.evaluation

import java.io.Serializable

import com.github.javacliparser.Configurable
import org.apache.spark.streamdm.core.DenseSingleLabelInstance
import org.apache.spark.streaming.dstream.DStream

abstract class Evaluator extends Configurable with Serializable{
  def addResult(input: DStream[(DenseSingleLabelInstance, Double)])
  def getResult():Double
}
