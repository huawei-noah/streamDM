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
import scala.collection.mutable.Map

package org.apache.spark.streamdm.core

/**
 * An ExampleSpecification contains information about the input and output features.
 * It contains a reference to an input InstanceSpecification and an output InstanceSpecification,
 * and provides setters and getters for the feature specification properties as name, type, and number of
 * features.
*/

class ExampleSpecification(inInstanceSpecification: InstanceSpecification,
                           outInstanceSpecification: InstanceSpecification) extends Serializable {

  val in = inInstanceSpecification
  val out = outInstanceSpecification

  /** Gets the input FeatureSpecification value present at position index
    *
    * @param index the index of the position
    * @return a FeatureSpecification representing the specification for the feature
    */
  def inputFeatureSpecification(index: Int): FeatureSpecification  = in(index)

  /** Gets the output FeatureSpecification value present at position index
    *
    * @param index the index of the position
    * @return a FeatureSpecification representing the specification for the feature
    */
  def outputFeatureSpecification(index: Int): FeatureSpecification  = out(index)

  /** Adds a specification of an input instance feature
    *
    * @param index the index at which the value is added
    * @param input the feature specification which is added up
    */
  def setInputFeatureSpecification(index: Int, input: FeatureSpecification):Unit = in.setFeatureSpecification(index, input)

  /** Adds a specification of an output instance feature
    *
    * @param index the index at which the value is added
    * @param input the feature specification which is added up
    */
  def setOutputFeatureSpecification(index: Int, input: FeatureSpecification):Unit = out.setFeatureSpecification(index, input)

  /** Gets if an input feature is numeric
    *
    * @param index the index of the feature
    * @return true if the feature is numeric
    */
  def isNumericInputFeature(index:Int):Boolean = in.isNumeric(index)

  /** Gets if an output feature is numeric
    *
    * @param index the index of the feature
    * @return true if the feature is numeric
    */
  def isNumericOutputFeature(index:Int):Boolean = out.isNumeric(index)

  /** Gets the input name of the feature at position index
    *
    * @param index the index of the class
    * @return a string representing the name of the feature
    */
  def nameInputFeature(index:Int):String = in.name(index)

  /** Gets the output name of the feature at position index
    *
    * @param index the index of the class
    * @return a string representing the name of the feature
    */
  def nameOutputFeature(index:Int):String = out.name(index)

  /** Gets the number of input features
    *
    * @return an Integer representing the number of input features
    */
  def numberInputFeatures(): Int = in.size

  /** Gets the number of output features
    *
    * @return an Integer representing the number of output features
    */
  def numberOutputFeatures(): Int = out.size
}

/**
 * An InstanceSpecification contains information about the features.
 * It returns information of features that are not numeric,
 * and the names of all features, numeric and discrete,
 */

class InstanceSpecification extends Serializable {
  val featureSpecificationMap = Map[Int, FeatureSpecification]()
  val nameMap = Map[Int, String]()

  /** Gets the FeatureSpecification value present at position index
    *
    * @param index the index of the position
    * @return a FeatureSpecification representing the specification for the feature
    */
  def apply(index: Int): FeatureSpecification = featureSpecificationMap(index)


  /** Adds a specification for the instance feature
    *
    * @param index the index at which the value is added
    * @param input the feature specification which is added up
    */
  def setFeatureSpecification(index: Int, input: FeatureSpecification): Unit =
    featureSpecificationMap += (index -> input)


  /** Gets if the feature is nominal or discrete
    *
    * @param index the index of the feature
    * @return true if the feature is discrete
    */
  def isNominal(index:Int):Boolean =
    featureSpecificationMap.contains(index)

  /** Gets if the feature is numeric
    *
    * @param index the index of the feature
    * @return true if the feature is numeric
    */
  def isNumeric(index:Int):Boolean =
    !isNominal(index)

  /** Gets the name of the feature at position index
    *
    * @param index the index of the class
    * @return a string representing the name of the feature
    */
  def name(index: Int): String = nameMap(index)

  /** Adds a name for the instance feature
    *
    * @param index the index at which the value is added
    * @param input the feature name which is added up
    */
  def setName(index: Int, input: String): Unit =
    nameMap += (index -> input)

  /** Gets the number of features
    *
    * @return the number of features
    */
  def size(): Int = nameMap.size
}

/**
 * A FeatureSpecification contains information about its nominal values.
 *
*/

class FeatureSpecification(nominalValues:Array[String]) extends Serializable {
  val values = nominalValues

  /** Get the nominal string value present at position index
    *
    * @param index the index of the feature value
    * @return a string containing the nominal value of the feature
    */
  def apply(index: Int): String = values(index)
}
