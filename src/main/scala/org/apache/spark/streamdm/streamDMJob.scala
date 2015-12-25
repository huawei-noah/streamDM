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

package org.apache.spark.streamdm

import org.apache.spark._
import org.apache.spark.streamdm.tasks.Task
import org.apache.spark.streaming._

import com.github.javacliparser.ClassOption

/**
 * The main entry point for testing StreamDM by running tasks on Spark
 * Streaming.
 */
object streamDMJob {

  def main(args: Array[String]) {

    //configuration and initialization of model
    val conf = new SparkConf().setAppName("streamDM")
    conf.setMaster("local[2]")

    val ssc = new StreamingContext(conf, Seconds(10))

    //run task
    val string = if (args.length > 0) args.mkString(" ") 
                  else "EvaluatePrequential"
    val task:Task = ClassOption.cliStringToObject(string, classOf[Task], null)
    task.run(ssc)

    //start the loop
    ssc.start()
    ssc.awaitTermination()
  }
}

