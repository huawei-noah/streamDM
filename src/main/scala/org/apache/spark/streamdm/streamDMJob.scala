package org.apache.spark.streamdm

import com.github.javacliparser.ClassOption
import org.apache.spark._
import org.apache.spark.streamdm.tasks.Task
import org.apache.spark.streaming._

object streamDMJob {

  def main(args: Array[String]) {

    //configuration and initialization of model
    val conf = new SparkConf().setAppName("streamDM")
    conf.setMaster("local[2]")

    val ssc = new StreamingContext(conf, Seconds(10))

    // Run Task
    val string = if (args.length > 0) args.mkString(" ") else "EvaluatePrequential"
    val task:Task = ClassOption.cliStringToObject(string, classOf[Task], null)
    task.run(ssc)

    //start the loop
    ssc.start()
    ssc.awaitTermination()
  }
}

