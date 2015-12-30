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

/**
 * An ExampleSpecification contains information about the input and output
 * features.  It contains a reference to an input InstanceSpecification and an
 * output InstanceSpecification, and provides setters and getters for the
 * feature specification properties.
 */

class ExampleSpecification(inInstanceSpecification: InstanceSpecification,
  outInstanceSpecification: InstanceSpecification)
    extends Serializable {

  val in = inInstanceSpecification
  val out = outInstanceSpecification

  /**
   * Gets the input FeatureSpecification value present at position index
   *
   * @param index the index of the specification
   * @return a FeatureSpecification representing the specification for the
   * feature
   */
  def inputFeatureSpecification(index: Int): FeatureSpecification = in(index)

  /**
   * Gets the output FeatureSpecification value present at position index
   *
   * @param index the index of the specification
   * @return a FeatureSpecification representing the specification for the
   * feature
   */
  def outputFeatureSpecification(index: Int): FeatureSpecification = out(index)

  /**
   * Evaluates whether an input feature is numeric
   *
   * @param index the index of the feature
   * @return true if the feature is numeric
   */
  def isNumericInputFeature(index: Int): Boolean = in.isNumeric(index)

  /**
   * Evaluates whether an output feature is numeric
   *
   * @param index the index of the feature
   * @return true if the feature is numeric
   */
  def isNumericOutputFeature(index: Int): Boolean = out.isNumeric(index)

  /**
   * Gets the input name of the feature at position index
   *
   * @param index the index of the class
   * @return a string representing the name of the feature
   */
  def nameInputFeature(index: Int): String = in.name(index)

  /**
   * Gets the output name of the feature at position index
   *
   * @param index the index of the class
   * @return a string representing the name of the feature
   */
  def nameOutputFeature(index: Int): String = out.name(index)

  /**
   * Gets the number of input features
   *
   * @return an Integer representing the number of input features
   */
  def numberInputFeatures(): Int = in.size

  /**
   * Gets the number of output features
   *
   * @return an Integer representing the number of output features
   */
  def numberOutputFeatures(): Int = out.size
}



