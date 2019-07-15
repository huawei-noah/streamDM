/*
 * Copyright (C) 2019 Télécom ParisTech LTCI lab.
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
import org.apache.spark.streamdm.evaluation.Evaluator
import org.apache.spark.streamdm.outlier.Outlier
import org.apache.spark.streamdm.streams._
import org.apache.spark.streaming.StreamingContext

/**
 * Task for evaluating a outlier (i.e., anomaly) detector on a stream by evaluating then training with
 * each example in sequence. The outlier detector method output either the property or degree of being an outlier
 * (i.e., outlierness score). A threshold is used to obtain the binary prediction (is_anomaly?) in case the
 * outlier detector outputs the degree of an example being an outlier. In case the outlier detector outputs the
 * property of an example being an outlier, then the values will be either 0.0 (normal) or 1.0 (outlier) and
 * the default threshold (t=0.5) will work as expected.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> outlierDetector (<b>-c</b>), an object of type <tt>Outlier</tt>
 *  <li> Evaluator (<b>-e</b>), an object of type <tt>Evaluator</tt>
 *  <li> Reader (<b>-s</b>), a reader object of type <tt>StreamReader</tt>
 *  <li> Writer (<b>-w</b>), a writer object of type <tt>StreamWriter</tt>
 *  <li> Threshold (<b>-t</b>), a value between 0.0 and Float.MaxValue of type <tt>Float</tt>
 * </ul>
 */
class EvaluateOutlierDetection extends Task {

  val outlierDetectorOption:ClassOption = new ClassOption("outlierDetector", 'o',
    "Outlier detector to use", classOf[Outlier], "SWNearestNeighbors")

  // The default behavior is to use the same evaluator as in classification tasks (i.e., BasicClassificationEvaluator)
  val evaluatorOption:ClassOption = new ClassOption("evaluator", 'e',
    "Evaluator to use", classOf[Evaluator], "BasicClassificationEvaluator")

  val streamReaderOption:ClassOption = new ClassOption("streamReader", 's',
    "Stream reader to use", classOf[StreamReader], "org.apache.spark.streamdm.streams.generators.RandomTreeGenerator")

  val resultsWriterOption:ClassOption = new ClassOption("resultsWriter", 'w',
    "Stream writer to use", classOf[StreamWriter], "PrintStreamWriter")

  val shouldPrintHeaderOption:FlagOption = new FlagOption("shouldPrintHeader", 'h',
    "Whether or not to print the evaluator header on the output file")

  val thresholdOutliernessOption: FloatOption = new FloatOption("thresholdOutlierness", 't',
    "threshold for given the outlierness value if the example is an outlier or not", 0.5, 0.0, Float.MaxValue)

  /**
   * Run the task.
   * @param ssc The Spark Streaming context in which the task is run.
   */
  def run(ssc:StreamingContext): Unit = {

    val reader:StreamReader = this.streamReaderOption.getValue()

    val outlierDetector:Outlier = this.outlierDetectorOption.getValue()
    outlierDetector.init(reader.getExampleSpecification())

    val evaluator:Evaluator = this.evaluatorOption.getValue()
    evaluator.setExampleSpecification(reader.getExampleSpecification())

    val writer:StreamWriter = this.resultsWriterOption.getValue()

    val threshold: Double = thresholdOutliernessOption.getValue()

    val examples = reader.getExamples(ssc)

    if(shouldPrintHeaderOption.isSet) {
      writer.output(evaluator.header())
    }

    // Obtain the outlierness of the examples and update the outlier detector
    val outlierness = outlierDetector.outlierness(examples)

    // Evaluate using the threshold parameter.
    writer.output(
      evaluator.addResult(
        outlierness.map( e => (e._1, if(e._2 > threshold) 1.0 else 0.0)
        )
      )
    )

  }
}
