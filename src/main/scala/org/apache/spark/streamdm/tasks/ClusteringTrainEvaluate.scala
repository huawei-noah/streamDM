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
import org.apache.spark.streamdm.evaluation.Evaluator

/**
 * Task for evaluating a clustering on a stream by first applying the clustering
 * and then evaluating the cluster statistics.
 * 
 * <p>It uses the following options:
 * <ul>
 *  <li> Clusterer (<b>-c</b>), an object of type <tt>Clusterer</tt>
 *  <li> Evaluator (<b>-e</b>), an object of type <tt>Evaluator</tt>
 *  <li> Reader (<b>-s</b>), a reader object of type <tt>StreamReader</tt>
 *  <li> Writer (<b>-w</b>), a writer object of type <tt>StreamWriter</tt>
 * </ul>
 */
class ClusteringTrainEvaluate extends Task {

  val clustererOption: ClassOption = new ClassOption("clusterer", 'c',
    "Clusterer to use", classOf[Clusterer], "Clustream")

  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "ClusteringCohesionEvaluator")

  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "SocketTextStreamReader")

  val resultsWriterOption:ClassOption = new ClassOption("resultsWriter", 'w',
    "Stream writer to use", classOf[StreamWriter], "PrintStreamWriter")
  /**
   * Run the task.
   * @param ssc The Spark Streaming context in which the task is run.
   */
  def run(ssc:StreamingContext): Unit = {

    val reader:StreamReader = this.streamReaderOption.getValue()

    val clusterer: Clusterer = this.clustererOption.getValue()
    clusterer.init(reader.getExampleSpecification())

    val evaluator:Evaluator = this.evaluatorOption.getValue()

    val writer:StreamWriter = this.resultsWriterOption.getValue()

    val instances = reader.getExamples(ssc)

    //Train
    clusterer.train(instances)

    //Assign
    val clpairs = clusterer.assign(instances)
    
    //Print statistics
    writer.output(evaluator.addResult(clpairs))
  }
}
