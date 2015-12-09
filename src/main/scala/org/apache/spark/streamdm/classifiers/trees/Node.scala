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

package org.apache.spark.streamdm.classifiers.trees

import scala.collection.mutable.ArrayBuffer
import scala.math.{ max }

import org.apache.spark.Logging

import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.bayes._
import org.apache.spark.streamdm.util.Util

/**
 * Abstract class containing the node information for the Hoeffding trees.
 */
abstract class Node(val classDistribution: Array[Double]) extends Serializable with Logging {

  var dep: Int = 0
  // stores class distribution of a block of RDD
  val blockClassDistribution: Array[Double] = new Array[Double](classDistribution.length)

  /**
   * Filter the data to the related leaf node
   *
   * @param example the input Example
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return a FoundNode containing the leaf node
   */
  def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode

  /**
   * Return the class distribution
   * @return an Array containing the class distribution
   */
  def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] =
    classDistribution.clone()

  /**
   * Checks whether a node is a leaf
   * @return <i>true</i> if a node is a leaf, <i>false</i> otherwise
   */
  def isLeaf(): Boolean = true

  /**
   * Returns height of the tree
   *
   * @return the height
   */
  def height(): Int = 0

  /**
   * Returns depth of current node in the tree
   *
   * @return the depth
   */
  def depth(): Int = dep

  /**
   * Set the depth of current node
   *
   * @param depth the new depth
   */
  def setDepth(depth: Int): Unit = {
    dep = depth
    if (this.isInstanceOf[SplitNode]) {
      val splidNode = this.asInstanceOf[SplitNode]
      splidNode.children.foreach { _.setDepth(depth + 1) }
    }
  }

  /**
   * Merge two nodes
   *
   * @param node the node which will be merged
   * @param trySplit flag indicating whether the node will be split
   * @return new node
   */
  def merge(that: Node, trySplit: Boolean): Node

  /**
   * Returns number of children
   *
   * @return number of children
   */
  def numChildren(): Int = 0

  /**
   * Returns the node description
   * @return String containing the description
   */
  def description(): String = {
    "  " * dep + "Leaf" + getClass().getName() + " weight = " +
      Util.arraytoString(classDistribution) + "\n"
  }

}

/**
 * The container of a node.
 */
class FoundNode(val node: Node, val parent: SplitNode, val index: Int) extends Serializable {

}

/**
 * Branch node of the Hoeffding tree.
 */
class SplitNode(classDistribution: Array[Double], val conditionalTest: ConditionalTest)
  extends Node(classDistribution) with Serializable {

  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()

  def this(that: SplitNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution),
      that.conditionalTest)
  }

  /**
   * Filter the data to the related leaf node
   *
   * @param example input example
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return FoundNode cotaining the leaf node
   */
  override def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode = {
    val cIndex = childIndex(example)
    if (cIndex >= 0) {
      if (cIndex < children.length && children(cIndex) != null) {
        children(cIndex).filterToLeaf(example, this, cIndex)
      } else new FoundNode(null, this, cIndex)
    } else new FoundNode(this, parent, index)
  }

  def childIndex(example: Example): Int = {
    conditionalTest.branch(example)
  }

  def setChild(index: Int, node: Node): Unit = {
    if (children.length > index) {
      children(index) = node
      node.setDepth(dep + 1)
    } else if (children.length == index) {
      children.append(node)
      node.setDepth(dep + 1)
    } else {
      assert(children.length < index)
    }
  }
  /**
   * Returns whether a node is a leaf
   */
  override def isLeaf() = false

  /**
   * Returns height of the tree
   *
   * @return the height
   */
  override def height(): Int = {
    var height = 0
    for (child: Node <- children) {
      height = max(height, child.height()) + 1
    }
    height
  }

  /**
   * Returns number of children
   *
   * @return  number of children
   */
  override def numChildren(): Int = children.filter { _ != null }.length

  /**
   * Merge two nodes
   *
   * @param node the node which will be merged
   * @param trySplit flag indicating whether the node will be split
   * @return new node
   */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (!that.isInstanceOf[SplitNode]) this
    else {
      val splitNode = that.asInstanceOf[SplitNode]
      for (i <- 0 until children.length)
        this.children(i) = (this.children(i)).merge(splitNode.children(i), trySplit)
      this
    }
  }

  /**
   * Returns the node description
   * @return String containing the description
   */
  override def description(): String = {
    val sb = new StringBuffer("  " * dep + "\n")
    val testDes = conditionalTest.description()
    for (i <- 0 until children.length) {
      sb.append("  " * dep + " if " + testDes(i) + "\n")
      sb.append("  " * dep + children(i).description())
    }
    sb.toString()
  }

  override def toString(): String = "level[" + dep + "] SplitNode"

}
/**
 * Learning node class type for Hoeffding trees.
 */
abstract class LearningNode(classDistribution: Array[Double]) extends Node(classDistribution)
  with Serializable {

  /**
   * Learn and update the node
   *
   * @param ht a Hoeffding tree model
   * @param example the input Example
   */
  def learn(ht: HoeffdingTreeModel, example: Example): Unit

  /**
   * Return whether a learning node is active
   */
  def isActive(): Boolean

  /**
   * Filter the data to the related leaf node
   *
   * @param example the input example
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return FoundNode containing the leaf node
   */
  override def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode =
    new FoundNode(this, parent, index)

}

/**
 * Basic majority class active learning node for Hoeffding tree
 */
class ActiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  var addonWeight: Double = 0

  var blockAddonWeight: Double = 0

  var instanceSpecification: InstanceSpecification = null

  var featureObservers: Array[FeatureClassObserver] = null

  def this(classDistribution: Array[Double], instanceSpecification: InstanceSpecification) {
    this(classDistribution)
    this.instanceSpecification = instanceSpecification
    init()
  }

  def this(that: ActiveLearningNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification)
    this.addonWeight = that.addonWeight
  }
  def init(): Unit = {
    if (featureObservers == null) {
      //featureObservers = new Array(instanceSpecification.size())
      featureObservers = new Array(7)
      for (i <- 0 until featureObservers.length) {
        val featureSpec: FeatureSpecification = instanceSpecification(i)
        featureObservers(i) = FeatureClassObserver.createFeatureClassObserver(
          classDistribution.length, i, featureSpec)
      }
    }
  }

  /**
   * Learn and update the node
   *
   * @param ht a Hoeffding tree model
   * @param example the input example
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {
    init()
    blockClassDistribution(example.labelAt(0).toInt) += example.weight
    featureObservers.zipWithIndex.foreach {
      x => x._1.observeClass(example.labelAt(0).toInt, example.featureAt(x._2), example.weight)
    }
  }
  /**
   * Disable a feature at a given index
   *
   * @param fIndex the index of the feature
   */
  def disableFeature(fIndex: Int): Unit = {

  }

  /**
   * Returns whether a node is active.
   *
   */
  override def isActive(): Boolean = true

  /**
   * Returns whether a node is pure, which means it only has examples belonging
   * to a single class.
   */
  def isPure(): Boolean = {
    this.classDistribution.filter(_ > 0).length <= 1 &&
      this.blockClassDistribution.filter(_ > 0).length <= 1
  }

  def weight(): Double = { classDistribution.sum + blockClassDistribution.sum }

  def blockWeight(): Double = blockClassDistribution.sum

  def addOnWeight(): Double = {
    addonWeight
  }

  /**
   * Merge two nodes
   *
   * @param node the node which will be merged
   * @param trySplit flag indicating whether the node will be split
   * @return new node
   */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (that.isInstanceOf[ActiveLearningNode]) {
      val node = that.asInstanceOf[ActiveLearningNode]
      logInfo("before: split?" + trySplit)
      logInfo("this:" + this.classDistribution.sum + "," + this.blockClassDistribution.sum)
      logInfo("that:" + node.classDistribution.sum + "," + node.blockClassDistribution.sum)
      //merge addonWeight and class distribution
      if (!trySplit) {
        this.blockAddonWeight += node.blockClassDistribution.sum
        for (i <- 0 until blockClassDistribution.length)
          this.blockClassDistribution(i) += that.blockClassDistribution(i)
      } else {
        this.addonWeight += node.blockAddonWeight
        for (i <- 0 until classDistribution.length)
          this.classDistribution(i) += node.blockClassDistribution(i)
      }
      //merge feature class observers
      for (i <- 0 until featureObservers.length)
        featureObservers(i) = featureObservers(i).merge(node.featureObservers(i), trySplit)
    }
    logInfo("merged:" + this.classDistribution.sum + "," + this.blockClassDistribution.sum)
    this
  }
  /**
   * Returns Split suggestions for all features.
   *
   * @param splitCriterion the SplitCriterion used
   * @param ht a Hoeffding tree model
   * @return an array of FeatureSplit
   */
  def getBestSplitSuggestions(splitCriterion: SplitCriterion, ht: HoeffdingTreeModel): Array[FeatureSplit] = {
    val bestSplits = new ArrayBuffer[FeatureSplit]()
    logInfo("fffsize:" + featureObservers.length)
    featureObservers.zipWithIndex.foreach(x => {
      val bestSplit = x._1.bestSplit(splitCriterion, classDistribution, x._2, ht.binaryOnly)
      if (bestSplit != null) {
        bestSplits.append(bestSplit)
      }
    })

    if (!ht.noPrePrune) {
      logInfo("ht.noPrePrune" + ht.noPrePrune)
      bestSplits.append(new FeatureSplit(null, splitCriterion.merit(classDistribution,
        Array.fill(1)(classDistribution)), new Array[Array[Double]](0)))
    }
    bestSplits.toArray
  }

  override def toString(): String = "level[" + dep + "]ActiveLearningNode:" + weight
}
/**
 * Inactive learning node for Hoeffding trees
 */
class InactiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  def this(that: InactiveLearningNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution))
  }

  /**
   * Learn and update the node. No action is taken for InactiveLearningNode
   *
   * @param ht HoeffdingTreeModel
   * @param example an Example will be processed
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {}

  /**
   * Return whether a learning node is active
   */
  override def isActive(): Boolean = false

  /**
   * Merge two nodes
   *
   * @param node the node which will be merged
   * @param trySplit flag indicating whether the node will be split
   * @return new node
   */
  override def merge(that: Node, trySplit: Boolean): Node = this

  override def toString(): String = "level[" + dep + "] InactiveLearningNode"
}
/**
 * Naive Bayes based learning node.
 */
class LearningNodeNB(classDistribution: Array[Double], instanceSpecification: InstanceSpecification)
  extends ActiveLearningNode(classDistribution, instanceSpecification) with Serializable {

  def this(that: LearningNodeNB) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification)
    //init()
  }

  /**
   * Returns the predicted class distribution
   *
   * @param ht a Hoeffding tree model
   * @param example  the Example to be evaluated
   * @return the predicted class distribution
   */
  override def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] = {
    if (weight() > ht.nbThreshold)
      NaiveBayes.predict(example, classDistribution, featureObservers)
    else super.classVotes(ht, example)
  }

  /**
   * Disable a feature having an index
   *
   * @param fIndex the index of the feature
   */
  override def disableFeature(fIndex: Int): Unit = {
    featureObservers(fIndex) = new NullFeatureClassObserver()
  }
}

/**
 * Adaptive Naive Bayes learning node.
 */

class LearningNodeNBAdaptive(classDistribution: Array[Double],
                             instanceSpecification: InstanceSpecification)
  extends ActiveLearningNode(classDistribution, instanceSpecification) with Serializable {

  var mcCorrectWeight: Double = 0
  var nbCorrectWeight: Double = 0

  var mcBlockCorrectWeight: Double = 0
  var nbBlockCorrectWeight: Double = 0

  def this(that: LearningNodeNBAdaptive) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification)
    addonWeight = that.addonWeight
    mcCorrectWeight = that.mcCorrectWeight
    nbCorrectWeight = that.nbCorrectWeight
    init()
  }

  /**
   * Learn and update the node.
   *
   * @param ht a Hoeffding tree model
   * @param example an input example
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {
    super.learn(ht, example)
    if (Util.argmax(classDistribution) == example.labelAt(0))
      mcBlockCorrectWeight += example.weight
    if (Util.argmax(NaiveBayes.predict(example, classDistribution, featureObservers)) ==
      example.labelAt(0))
      nbBlockCorrectWeight += example.weight
  }

  /**
   * Merge two nodes
   *
   * @param node the node which will be merged
   * @param trySplit flag indicating whether the node will be split
   * @return new node
   */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (that.isInstanceOf[LearningNodeNBAdaptive]) {
      val nbaNode = that.asInstanceOf[LearningNodeNBAdaptive]
      //merge weights and class distribution
      if (!trySplit) {
        this.blockAddonWeight += nbaNode.blockClassDistribution.sum
        mcBlockCorrectWeight += nbaNode.mcBlockCorrectWeight
        nbBlockCorrectWeight += nbaNode.nbBlockCorrectWeight
        for (i <- 0 until blockClassDistribution.length)
          this.blockClassDistribution(i) += that.blockClassDistribution(i)
      } else {
        this.addonWeight += nbaNode.blockAddonWeight
        mcCorrectWeight += nbaNode.mcBlockCorrectWeight
        nbCorrectWeight += nbaNode.nbBlockCorrectWeight
        for (i <- 0 until classDistribution.length)
          this.classDistribution(i) += that.blockClassDistribution(i)
      }
      //merge feature class observers
      for (i <- 0 until featureObservers.length)
        featureObservers(i) = featureObservers(i).merge(nbaNode.featureObservers(i), trySplit)

    }
    this
  }

  /**
   * Returns the predicted class distribution
   *
   * @param ht a Hoeffding tree model
   * @param example the input example
   * @return the predicted class distribution
   */
  override def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] = {
    if (mcCorrectWeight > nbCorrectWeight) super.classVotes(ht, example)
    else NaiveBayes.predict(example, classDistribution, featureObservers)
  }
}
