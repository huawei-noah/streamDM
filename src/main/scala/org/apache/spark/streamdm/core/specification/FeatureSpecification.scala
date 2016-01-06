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
 * trait FeatureSpecification.
 *
 */
trait FeatureSpecification {

  /**
   * whether the feature is nominal
   *
   * @return true if the feature is nominal
   */
  def isNominal(): Boolean

  /**
   * whether the feature is numeric
   *
   * @return true if the feature is discrete
   */
  def isNumeric(): Boolean
  /**
   * if a feature is numeric, return -1, else return the nominal values size
   */
  def range(): Int
}

/**
 * class NumericFeatureSpecification.
 *
 */
class NumericFeatureSpecification extends FeatureSpecification with Serializable {
  /**
   * whether the feature is nominal
   *
   * @return true if the feature is nominal
   */
  override def isNominal(): Boolean = false
  /**
   * whether the feature is numeric
   *
   * @return true if the feature is discrete
   */
  override def isNumeric(): Boolean = !isNominal()
  override def range(): Int = -1
}

/**
 * A NominalFeatureSpecification contains information about its nominal values.
 *
 */

class NominalFeatureSpecification(nominalValues: Array[String]) extends FeatureSpecification with Serializable {
  val values = nominalValues

  val nominalToNumericMap = Map[String,Int]()
  values.zipWithIndex.map{ case (element, index) => 
                            (nominalToNumericMap += (element -> (index))) }

  /** Get the nominal string value present at position index
    *
    * @param index the index of the feature value
    * @return a string containing the nominal value of the feature
    */
  def apply(index: Int): String =  values(index)

  /** Get the position index given the nominal string value
    *
    * @param string a string containing the nominal value of the feature
    * @return the index of the feature value
    */
  def apply(string: String): Int = nominalToNumericMap(string)

  /**
   * whether the feature is nominal
   *
   * @return true if the feature is nominal
   */
  override def isNominal(): Boolean = true
  /**
   * whether the feature is numeric
   *
   * @return true if the feature is discrete
   */
  override def isNumeric(): Boolean = !isNominal()
  /**
   * return the nominal values size
   */
  override def range(): Int = values.length
}
