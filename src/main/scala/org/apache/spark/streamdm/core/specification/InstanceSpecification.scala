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

import scala.collection.mutable.Map

/**
 * An InstanceSpecification contains information about the features.
 * It returns information of features that are not numeric,
 * and the names of all features, numeric and discrete,
 */

class InstanceSpecification extends Serializable {
  val nominalFeatureSpecificationMap = Map[Int, FeatureSpecification]()
  val featureNameMap = Map[Int, String]()
  val numericFeatureSpecification: NumericFeatureSpecification = new NumericFeatureSpecification

  /**
   * Gets the FeatureSpecification value present at position index
   *
   * @param index the index of the position
   * @return a FeatureSpecification representing the specification for the
   * feature
   */
  def apply(index: Int): FeatureSpecification = {
    if (nominalFeatureSpecificationMap.contains(index))
      nominalFeatureSpecificationMap(index)
    else numericFeatureSpecification
  }

  /**
   * Removes the FeatureSpecification value present at position index
   *
   * @param index the index of the position
   * @return Unit
   */
  def removeFeatureSpecification(index: Int): Unit = {
    if (nominalFeatureSpecificationMap.contains(index))
      nominalFeatureSpecificationMap.remove(index)
    featureNameMap.remove(index)
  }

  /**
   * Adds a specification for the instance feature
   *
   * @param index the index at which the value is added
   * @param input the feature specification which is added up
   */
  def addFeatureSpecification(index: Int, name: String, fSpecification: FeatureSpecification = null): Unit = {
    if (fSpecification != null && fSpecification.isInstanceOf[NominalFeatureSpecification]) {
      nominalFeatureSpecificationMap += (index -> fSpecification)
    }
    featureNameMap += (index -> name)
  }
  /**
   * Evaluates whether a feature is nominal or discrete
   *
   * @param index the index of the feature
   * @return true if the feature is discrete
   */
  def isNominal(index: Int): Boolean =
    this(index).isNominal()

  /**
   * Evaluates whether a feature is numeric
   *
   * @param index the index of the feature
   * @return true if the feature is numeric
   */
  def isNumeric(index: Int): Boolean =
    !isNominal(index)

  /**
   * Gets the name of the feature at position index
   *
   * @param index the index of the class
   * @return a string representing the name of the feature
   */
  def name(index: Int): String = featureNameMap(index)

  /**
   * Gets the number of features
   *
   * @return the number of features
   */
  def size(): Int = featureNameMap.size
}
