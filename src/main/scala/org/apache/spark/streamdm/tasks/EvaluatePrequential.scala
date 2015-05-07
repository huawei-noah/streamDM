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

import com.github.javacliparser.{StringOption, ClassOption}
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers._
import org.apache.spark.streamdm.streams._
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streamdm.evaluation.Evaluator

/**
 * Task for evaluating a classifier on a stream by testing then training with each example in sequence.
 *
 */
class EvaluatePrequential extends Task {

  val learnerOption:ClassOption = new ClassOption("learner", 'l',
    "Learner to use", classOf[Learner], "SGDLearner")

  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")

  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "SocketTextStreamReader")

  val textOption:StringOption = new StringOption("text", 't',
    "Text to print", "Text")

  def run(ssc:StreamingContext): Unit = {

    val learner:SGDLearner = this.learnerOption.getValue()
    learner.init()
    val evaluator:Evaluator = this.evaluatorOption.getValue()

    val reader:StreamReader = this.streamReaderOption.getValue()

    val instances = reader.getInstances(ssc)

    print (textOption.getValue)

    //Predict
    val predPairs = learner.predict(instances)

    //Train
    learner.train(instances)

    //Evaluate
    evaluator.addResult(predPairs)

  }
}
