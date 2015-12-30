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

package org.apache.spark.streamdm.core.specification

import scala.io.Source
import org.apache.spark.streamdm.classifiers.trees.Utils.{ arraytoString }

/*
 * object SpecificationHead helps to generate head for data
 */
object SpecificationHead {

  /*
   * Get string head from ExampleSpecification by head type, which will be saved to head file.
   * 
   * @param spec ExampleSpecification
   * @param t data head type
   * @return string of head
   * 
   */
  def getHead(spec: ExampleSpecification, t: String): String = t match {
    case "arff" => toArff(spec)
    case "csv" => toCsv(spec)
    case _ => toArff(spec)
  }

  /*
   * Get ExampleSpecification from head file by head type.
   * 
   * @param fileName name of head file
   * @param t data head type
   * @return ExampleSpecification of data
   * 
   */
  def getSpecification(fileName: String, t: String): ExampleSpecification = t match {
    case "arff" => fromArff(fileName)
    case "csv" => fromCsv(fileName)
    case _ => fromArff(fileName)
  }

  def toArff(spec: ExampleSpecification): String = {
    val sb = new StringBuffer()
    sb.append("@relation sample-data\n")
    val inputIS = spec.in
    val outputIs = spec.out
    val atr = "@attribute "
    val nu = "numeric"
    // add arff attributes of input
    for (index <- 0 until inputIS.size()) {
      val featureName: String = inputIS.name(index)
      val featureSpec: FeatureSpecification = inputIS(index)

      val line = featureSpec match {
        case numeric: NumericFeatureSpecification => { nu }
        case nominal: NominalFeatureSpecification => { arraytoString(nominal.values) }
      }
      sb.append(atr + featureName + " " + line + "\n")
    }
    // add arff attributes of outnput
    sb.append(atr + outputIs.name(0) + " " +
      arraytoString(outputIs(0).asInstanceOf[NominalFeatureSpecification].values))
    sb.toString()
  }

  def fromArff(fileName: String): ExampleSpecification = {
    val lines = Source.fromFile(fileName).getLines()
    var line: String = lines.next()
    while (line == null || line.length() == 0 || line.startsWith(" ") ||
      line.startsWith("%")) {
      line = lines.next()
    }
    var finished: Boolean = false
    var index: Int = 0
    val inputIS = new InstanceSpecification()
    val outputIS = new InstanceSpecification()
    while (!finished && line.startsWith("@")) {
      if (line.startsWith("@data")) {
        val fSpecification: FeatureSpecification = inputIS(index - 1)
        outputIS.addFeatureSpecification(0, "class", fSpecification)
        inputIS.removeFeatureSpecification(index - 1)
        finished = true
      } else if (line.startsWith("@attribute")) {
        val featureInfos: Array[String] = line.split(" ")
        val name: String = featureInfos(1)
        if (!featureInfos(2).equals("numeric")) {
          val fSpecification = new NominalFeatureSpecification(
            featureInfos(2).substring(0, featureInfos(2).length - 1).split(","))
          inputIS.addFeatureSpecification(index, "Norminal" + index, fSpecification)
        } else {
          inputIS.addFeatureSpecification(index, "Numeric" + index)
        }

      }
      index += 1
    }

    new ExampleSpecification(inputIS, outputIS)

  }

  def toCsv(spec: ExampleSpecification): String = {
    //todo
    ""
  }

  def fromCsv(fileName: String): ExampleSpecification = {
    //todo
    null
  }

}