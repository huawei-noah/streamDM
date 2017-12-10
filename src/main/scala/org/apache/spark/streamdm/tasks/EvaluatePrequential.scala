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

import com.github.javacliparser._
import org.apache.spark.streamdm.classifiers._
import org.apache.spark.streamdm.streams._
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streamdm.evaluation.Evaluator

/**
 * Task for evaluating a classifier on a stream by testing then training with
 * each example in sequence.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Learner (<b>-c</b>), an object of type <tt>Classifier</tt>
 *  <li> Evaluator (<b>-e</b>), an object of type <tt>Evaluator</tt>
 *  <li> Reader (<b>-s</b>), a reader object of type <tt>StreamReader</tt>
 *  <li> Writer (<b>-w</b>), a writer object of type <tt>StreamWriter</tt>
 * </ul>
 */
class EvaluatePrequential extends Task {

  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Classifier], "SGDLearner")

  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")

  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "org.apache.spark.streamdm.streams.generators.RandomTreeGenerator")

  val resultsWriterOption:ClassOption = new ClassOption("resultsWriter", 'w',
    "Stream writer to use", classOf[StreamWriter], "PrintStreamWriter")

  val shouldPrintHeaderOption:FlagOption = new FlagOption("shouldPrintHeader", 'h',
    "Whether or not to print the evaluator header on the output file")

  /**
   * Run the task.
   * @param ssc The Spark Streaming context in which the task is run.
   */
  def run(ssc:StreamingContext): Unit = {

    val reader:StreamReader = this.streamReaderOption.getValue()

    val learner:Classifier = this.learnerOption.getValue()
    learner.init(reader.getExampleSpecification())

    val evaluator:Evaluator = this.evaluatorOption.getValue()
    evaluator.setExampleSpecification(reader.getExampleSpecification())

    val writer:StreamWriter = this.resultsWriterOption.getValue()

    val instances = reader.getExamples(ssc)

    if(shouldPrintHeaderOption.isSet) {
      writer.output(evaluator.header())
    }

    //Predict
    val predPairs = learner.predict(instances)

    //Train
    learner.train(instances)

    //Evaluate
    writer.output(evaluator.addResult(predPairs))

  }
}