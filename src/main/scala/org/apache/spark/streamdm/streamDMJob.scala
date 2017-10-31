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

import scala.util.Try

/**
 * The main entry point for testing StreamDM by running tasks on Spark
 * Streaming.
 */
object streamDMJob {

  def main(args: Array[String]) {

    //configuration and initialization of model
    val conf = new SparkConf().setAppName("streamDM")

    var paramsArgs = args.clone()
    var batchInterval: Int = 1000
    if(args.length > 0){
      val firstArg = args(0)
      if(Try(firstArg.toInt).isSuccess){
        if(firstArg.toInt > 0 && firstArg.toInt < Int.MaxValue){
          batchInterval = firstArg.toInt
        }
        paramsArgs = paramsArgs.drop(1)
      }
    }

    val ssc = new StreamingContext(conf, Milliseconds(batchInterval))

    //run task
    val string = if (paramsArgs.length > 0) paramsArgs.mkString(" ")
    else "EvaluatePrequential"
    val task:Task = ClassOption.cliStringToObject(string, classOf[Task], null)
    task.run(ssc)

    //start the loop
    ssc.start()
    ssc.awaitTermination()
  }
}

