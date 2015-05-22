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

package org.apache.spark.streamdm.tasks

import com.github.javacliparser.ClassOption
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.clusterers._
import org.apache.spark.streamdm.streams._
import org.apache.spark.streaming.StreamingContext

/**
 * Task for debugging a clusterer
 *
 */
class ClustreamOutput extends Task {

  val clustererOption: ClassOption = new ClassOption("clusterer", 'c',
    "Clusterer to use", classOf[Clusterer], "Clustream")

  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "SocketTextStreamReader")

  def run(ssc:StreamingContext): Unit = {

    val clusterer: Clusterer = this.clustererOption.getValue()
    clusterer.init

    val reader:StreamReader = this.streamReaderOption.getValue()

    val instances = reader.getInstances(ssc)

    //Train
    clusterer.train(instances)

    //Assign
    val clpairs = clusterer.assign(instances)
    
    //Print
    clpairs.print
    /*
    clpairs.foreach(x=> {
      println("%s -> %s".format(x._1,x._2))
    })
    */


  }
}
