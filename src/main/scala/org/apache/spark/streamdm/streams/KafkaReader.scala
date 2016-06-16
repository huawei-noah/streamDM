package org.apache.spark.streamdm.streams
import com.github.javacliparser.StringOption
import kafka.serializer.StringDecoder
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.core.specification.{ExampleSpecification, InstanceSpecification, NominalFeatureSpecification}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils

/**
  * Created by wzq on 2016/6/16.
  */
class KafkaReader extends StreamReader{

  /** kafka brokers */
  val brokersOption: StringOption = new StringOption("brokers", 'b', "kafka brokers", "unset")
  /** topics name */
  val topicsOption: StringOption = new StringOption("topics", 'p', "topics name", "unset")
  /** data format*/
  val instanceOption: StringOption = new StringOption("instanceType", 't', "Type of the instance to use", "dense")

  /**
    * Obtains a stream of examples.
    *
    * @param ssc a Spark Streaming context
    * @return a stream of Examples
    */
  override def getExamples(ssc: StreamingContext): DStream[Example] = {
    val brokers = brokersOption.getValue
    val topics = topicsOption.getValue

    assert(!(brokers+topics).contains("unset"),"brokers or topics should be set")

    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val topicMap = topics.split(",").toSet

    KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc,kafkaParams,topicMap)
      .map(x=>Example.parse(x._2, instanceOption.getValue, "dense"))

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
    outputIS.addFeatureSpecification(0, "class", classFeature)

    new ExampleSpecification(new InstanceSpecification(), outputIS)
  }
}
