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

package org.apache.spark.streamdm.core

import org.apache.spark.internal.Logging
import org.apache.spark.streamdm.core.specification._

/*
 * object ExampleParser helps to parse example from/to different data format.
 */
object ExampleParser extends Logging {

  /*
   * create Example from arff format string
   * 
   * @param e arff format string
   * @spec ExampleSpecification
   * @return Example
   */
  def fromArff(e: String, spec: ExampleSpecification): Example = {
    var input: String = e
    var output: String = null
    val isSparse: Boolean = e.contains("{") && e.contains("}")
    if (isSparse) {
      // sparse data  
      input = e.substring(e.indexOf("{") + 1, e.indexOf("}")).trim()
    }
    if (spec.out.size() == 1) {
      val pos = input.lastIndexOf(",")
      output = input.substring(pos + 1).trim()
      input = input.substring(0, pos)
    }
    new Example(arffToInstace(input, spec.in, isSparse), arffToInstace(output, spec.out, isSparse))
  }

  /*
   * create Instance from arff format string
   * 
   * @param e arff format string
   * @spec InstanceSpecification
   * @return Instance
   */
  private def arffToInstace(data: String, spec: InstanceSpecification, isSparse: Boolean): Instance = {
    if (data == null || data.length() == 0) null
    else {
      if (isSparse)
        arffToSparceInstace(data, spec)
      else
        arffToDenseInstace(data, spec)
    }
  }

  /*
   * create SparseInstance from arff format string
   * 
   * @param e arff format string
   * @spec InstanceSpecification
   * @return SparseInstance
   */
  private def arffToSparceInstace(data: String, spec: InstanceSpecification): SparseInstance = {
    val tokens = data.split(",[\\s]?")
    val values = Array[Double](tokens.length)
    val features = tokens.map(_.split("\\s+"))
    for (index <- 0 until tokens.length) {
      values(index) = spec(index) match {
        case nominal: NominalFeatureSpecification => nominal(features(index)(1))
        case _ => features(index)(1).toDouble
      }
    }
    new SparseInstance(features.map(_(0).toInt), values)
  }

  /*
   * create DenseInstance from arff format string
   * 
   * @param e arff format string
   * @spec InstanceSpecification
   * @return DenseInstance
   */
  private def arffToDenseInstace(data: String, spec: InstanceSpecification): DenseInstance = {
    val stringValues = data.split(",[\\s]?")
    val values = new Array[Double](stringValues.length)
    for (index <- 0 until stringValues.length) {
      values(index) = spec(index) match {
        case nominal: NominalFeatureSpecification => nominal(stringValues(index))
        case _ => stringValues(index).toDouble
      }
    }
    new DenseInstance(values)
  }

  /*
   * create arff format string for Example
   * 
   * @param e Exmaple
   * @spec ExampleSpecification
   * @return arff format string
   */
  def toArff(e: Example, spec: ExampleSpecification): String = {
    val isSparse = e.in.isInstanceOf[SparseInstance]
    val sb = new StringBuffer()
    sb.append(instanceToArff(e.in, spec.in, 0))
    if (spec.out.size() != 0)
      sb.append(", " + instanceToArff(e.out, spec.out, spec.in.size()))
    sb.delete(sb.lastIndexOf(","), sb.length())
    if (isSparse) {
      sb.insert(0, "{")
      sb.append("}")
    }
    sb.toString()
  }
  /*
   * create part arff format string for Instance
   * 
   * @param instance: Instance
   * @spec ExampleSpecification
   * @return part arff format string
   */
  def instanceToArff(instance: Instance, insSpec: InstanceSpecification, startIndex: Int): String = {
    val isSparse = instance.isInstanceOf[SparseInstance]
    val sb = new StringBuffer()
    instance.getFeatureIndexArray().foreach(token =>
      {
        if (isSparse) {
          sb.append(startIndex + token._2 + " ")
        }
        val value = insSpec(token._2) match {
          case nominal: NominalFeatureSpecification => nominal(token._1.toInt)
          case numeric: NumericFeatureSpecification => token._1
        }
        sb.append(value + ", ")
      })
    sb.toString()
  }

}