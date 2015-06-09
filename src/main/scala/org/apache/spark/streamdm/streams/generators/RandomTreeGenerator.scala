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
package org.apache.spark.streamdm.streams.generators

import scala.collection.immutable.List
import com.github.javacliparser.{ IntOption, FloatOption }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader
import org.apache.spark.streaming.{ Duration, Time, StreamingContext }
import org.apache.spark.streaming.dstream.{ InputDStream, DStream }

import scala.util.Random

/**
 * Stream generator for generating data from a randomly generated tree.
 *
 */

class RandomTreeGenerator extends StreamReader {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'c',
    "Chunk Size", 1000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  //  val treeRandomSeedOption: IntOption = new IntOption("treeRandomSeed",
  //    'r', "Seed for random generation of tree.", 1);

  //  val instanceRandomSeedOption = new IntOption(
  //    "instanceRandomSeed", 'i',
  //    "Seed for random generation of instances.", 1)

  val numClassesOption = new IntOption("numClasses", 'n', //default is 'c'
    "The number of classes to generate.", 2, 2, Integer.MAX_VALUE)

  val numNominalsOption = new IntOption("numNominals", 'o',
    "The number of nominal attributes to generate.", 5, 0,
    Integer.MAX_VALUE)

  val numNumericsOption = new IntOption("numNumerics", 'u',
    "The number of numeric attributes to generate.", 5, 0,
    Integer.MAX_VALUE)

  val numValsPerNominalOption = new IntOption("numValsPerNominal", 'v',
    "The number of values to generate per nominal attribute.", 5, 2,
    Integer.MAX_VALUE)

  val maxTreeDepthOption = new IntOption("maxTreeDepth", 'x',
    "The maximum depth of the tree concept.", 5, 0, Integer.MAX_VALUE)

  val firstLeafLevelOption = new IntOption("firstLeafLevel", 'l',
    "The first level of the tree above maxTreeDepth that can have leaves.",
    3, 0, Integer.MAX_VALUE)

  val leafFractionOption = new FloatOption("leafFraction", 'r',
    "The fraction of leaves per level from firstLeafLevel onwards.",
    0.15, 0.0, 1.0)

  var treeRoot: Node = null

  /**
   * Obtains a stream of Examples
   * @param ssc a Spark Streaming Context
   * @return a stream of Examples
   */
  def getExamples(ssc: StreamingContext): DStream[Example] = {
    if (treeRoot == null) {
      init()
    }
    new InputDStream[Example](ssc) {

      override def start(): Unit = {}

      override def stop(): Unit = {}

      override def compute(validTime: Time): Option[RDD[Example]] = {
        val examples: Array[Example] = Array.fill[Example](chunkSizeOption.getValue)(getExample())
        Some(ssc.sparkContext.parallelize(examples))
      }

      override def slideDuration = {
        new Duration(slideDurationOption.getValue)
      }

      def getExample(): Example = {
        val featureVals = new Array[Double](numNominalsOption.getValue()
          + numNumericsOption.getValue())

        for (i <- 0 until featureVals.length) {
          featureVals(i) =
            if (i < numNominalsOption.getValue())
              Random.nextInt(numValsPerNominalOption.getValue())
            else Random.nextDouble()
        }
        val inputInstance = new DenseInstance(featureVals)
        //    val noiseInstance = new DenseInstance(Array.fill[Double](numFeaturesOption.getValue)(getNoise()))
        new Example(inputInstance, new DenseInstance(Array[Double](getLabel(treeRoot, featureVals))))
      }

      def getLabel(node: Node, featureVals: Array[Double]): Int = node match {
        case leaf: LeafNode => leaf.label
        case branch: BranchNode => {
          if (branch.fIndex < numNominalsOption.getValue()) {
            getLabel(
              branch.children(featureVals(branch.fIndex).toInt), featureVals); ;
          } else getLabel(
            branch.children(if (featureVals(branch.fIndex) < branch.fValue) 0 else 1), featureVals)
        }
      }

      //      def getExample(): Example = {
      //        val inputInstance = new DenseInstance(Array.fill[Double](numFeaturesOption.getValue)(5.0 * getRandomNumber()))
      //        val noiseInstance = new DenseInstance(Array.fill[Double](numFeaturesOption.getValue)(getNoise()))
      //        new Example(inputInstance.add(noiseInstance), new DenseInstance(Array.fill[Double](1)(label(inputInstance))))
      //      }

      //      def getRandomNumber(): Double = 2.0 * Random.nextDouble() - 1.0 // Uniform number between -1 and 1
      //
      //      def getNoise(): Double = 0.5 * Random.nextGaussian()
      //
      //      val weight = new DenseInstance(Array.fill[Double](numFeaturesOption.getValue)(getRandomNumber()))
      //
      //      val bias: Double = getRandomNumber()
      //
      //      def label(inputInstance: Instance): Double = {
      //        val sum = weight.dot(inputInstance)
      //        if (sum > bias) 1
      //        else 0
      //      }
    }
  }

  def init(): Unit = { generateRandomTree() }

  /**
   * Obtains the specification of the examples in the stream
   * @return an specification of the examples
   */
  def getExampleSpecification(): ExampleSpecification = {

    //Prepare specification of class feature
    val outputIS = new InstanceSpecification()
    val classFeature = new NominalFeatureSpecification(Array("+", "-"))
    outputIS.setFeatureSpecification(0, classFeature)
    outputIS.setName(0, "class")

    //Prepare specification of input Nominal features for 
    val inputIS = new InstanceSpecification()
    val nominal = new NominalFeatureSpecification(Array.range(0, numValsPerNominalOption.getValue).map { _.toString() })
    for (i <- 0 until numNominalsOption.getValue) {
      inputIS.setFeatureSpecification(i, nominal)
      inputIS.setName(i, "Feature" + i)
    }
    
    for (i <- numNominalsOption.getValue until numNominalsOption.getValue + numNumericsOption.getValue) {
      inputIS.setName(i, "Feature" + i)
    }

    new ExampleSpecification(inputIS, outputIS)
  }

  def generateRandomTree(): Unit = {
    val minNumericVals: Array[Double] = Array.fill(numNumericsOption.getValue())(0.0)
    val maxNumericVals: Array[Double] = Array.fill(numNumericsOption.getValue())(1.0)
    treeRoot = generateRandomTreeNode(0, List.range(0, numNominalsOption.getValue),
      minNumericVals, maxNumericVals);
  }

  def generateRandomTreeNode(currentDepth: Int, nominalFeatureCandidates: List[Int], minNumericVals: Array[Double],
                             maxNumericVals: Array[Double]): Node = {
    if ((currentDepth >= this.maxTreeDepthOption.getValue())
      || ((currentDepth >= this.firstLeafLevelOption.getValue()) && (this.leafFractionOption.getValue() >= (1.0 - Random.nextDouble())))) {
      val label = Random.nextInt(this.numClassesOption.getValue())
      new LeafNode(label)
    } else {
      val chosenFeature = Random.nextInt(nominalFeatureCandidates.length
        + this.numNumericsOption.getValue());
      if (chosenFeature < nominalFeatureCandidates.length) {
        val splitFeatureIndex = nominalFeatureCandidates(chosenFeature)
        val node = new BranchNode(splitFeatureIndex, numValsPerNominalOption.getValue())
        val newNominalCandidates = nominalFeatureCandidates.filter { _ != splitFeatureIndex }
        for (i <- 0 until numValsPerNominalOption.getValue()) {
          node.children(i) = generateRandomTreeNode(currentDepth + 1,
            newNominalCandidates, minNumericVals, maxNumericVals);
        }
        node
      } else {
        val numericIndex = chosenFeature - nominalFeatureCandidates.length;
        val splitFeatureIndex = this.numNominalsOption.getValue() + numericIndex;
        val minVal = minNumericVals(numericIndex);
        val maxVal = maxNumericVals(numericIndex);
        val splitFeatureValue = ((maxVal - minVal) * Random.nextDouble())
        +minVal;
        val node = new BranchNode(splitFeatureIndex, 2, splitFeatureValue)
        val newMaxVals = maxNumericVals.clone()
        newMaxVals(numericIndex) = splitFeatureValue
        node.children(0) = generateRandomTreeNode(currentDepth + 1,
          nominalFeatureCandidates, minNumericVals, newMaxVals)
        val newMinVals = minNumericVals.clone()
        newMinVals(numericIndex) = splitFeatureValue
        node.children(1) = generateRandomTreeNode(currentDepth + 1,
          nominalFeatureCandidates, newMinVals, maxNumericVals)
        node
      }
    }
  }

}

sealed abstract class Node

case class BranchNode(val fIndex: Int, numChild: Int, val fValue: Double = 0) extends Node with Serializable {
  val children = if (numChild <= 0) null else new Array[Node](numChild)
}

case class LeafNode(val label: Int) extends Node with Serializable {

}
