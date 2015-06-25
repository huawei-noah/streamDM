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

import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.bayes._
import org.apache.spark.streamdm.util.Util

/**
 * class Node for hoeffding Tree
 */
abstract class Node(val classDistribution: Array[Double]) extends Serializable {

  var dep: Int = 0
  // stores class distribution of a block of RDD
  val blockClassDistribution: Array[Double] = new Array[Double](classDistribution.length)

  /**
   * filter the data to the related leaf node
   *
   * @param example an Example will be processed
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return a FoundNode contains the leaf node
   */
  def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode

  /**
   * return the class distribution
   */
  def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] = classDistribution.clone()

  /**
   * whether a node is a leaf
   */
  def isLeaf(): Boolean = true

  /**
   * Returns depth of current node in the tree
   */
  def depth(): Int = dep

  /**
   * Set the depth of current node
   *
   * @param depth int value
   * @return Unit
   */
  def setDepth(depth: Int): Unit = {
    dep = depth
    if (this.isInstanceOf[SplitNode]) {
      val splidNode = this.asInstanceOf[SplitNode]
      splidNode.children.foreach { _.setDepth(depth + 1) }
    }
  }

  /**
   * merge the two node
   *
   * @param node the node which will be merged
   * @param trySplit whether the Hoeffding Tree is trying to split
   * @return current node
   */
  def merge(that: Node, trySplit: Boolean): Node

  /**
   * Returns number of children
   *
   * @return number of children
   */
  def numChildren(): Int = 0

  /**
   * node description
   */
  def description(): String = { "  " * dep + "Leaf" + " weight = " + Util.arraytoString(classDistribution) + "\n" }

}

/**
 * class FoundNode is the container of a node and has an index and a link to parent
 */
class FoundNode(val node: Node, val parent: SplitNode, val index: Int) extends Serializable {

}

/**
 * class SplitNode is a branch node for Hoeffding Tree
 */
class SplitNode(classDistribution: Array[Double], val conditionalTest: ConditionalTest)
  extends Node(classDistribution) with Serializable {

  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()

  def this(that: SplitNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution), that.conditionalTest)
  }

  /**
   * filter the data to the related leaf node
   *
   * @param example an Example will be processed
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return a FoundNode contains the leaf node
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
   * Returns number of children
   *
   * @return  number of children
   */
  override def numChildren(): Int = children.filter { _ != null }.length

  /**
   * merge the two node
   *
   * @param node the node which will be merged
   * @param trySplit whether the Hoeffding Tree is trying to split
   * @return current node
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
   * node description
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
 * class learning node for Hoeffding Tree
 */
abstract class LearningNode(classDistribution: Array[Double]) extends Node(classDistribution) with Serializable {

  /**
   * lean and update the node
   *
   * @param ht HoeffdingTreeModel
   * @param example an Example will be processed
   * @return Unit
   */
  def learn(ht: HoeffdingTreeModel, example: Example): Unit

  /**
   * whether a learning node is active
   *
   * @return Boolean value
   */
  def isActive(): Boolean

  /**
   * filter the data to the related leaf node
   *
   * @param example an Example will be processed
   * @param parent the parent of current node
   * @param index the index of current node in the parent children
   * @return a FoundNode contains the leaf node
   */
  override def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode = new FoundNode(this, parent, index)

}

/**
 * basic majority class active learning node for hoeffding tree
 */
class ActiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  var addonWeight: Double = 0

  var instanceSpecification: InstanceSpecification = null

  var featureObservers: Array[FeatureClassObserver] = null

  def this(classDistribution: Array[Double], instanceSpecification: InstanceSpecification) {
    this(classDistribution)
    this.instanceSpecification = instanceSpecification
    init()
  }

  def this(that: ActiveLearningNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution), that.instanceSpecification)

  }
  def init(): Unit = {
    if (featureObservers == null) {
      featureObservers = new Array(instanceSpecification.size())
      for (i <- 0 until instanceSpecification.size()) {
        val featureSpec: FeatureSpecification = instanceSpecification(i)
        featureObservers(i) = FeatureClassObserver.createFeatureClassObserver(classDistribution.length, i, featureSpec)
      }
    }
  }

  /**
   * lean and update the node
   *
   * @param ht HoeffdingTreeModel
   * @param example an Example will be processed
   * @return Unit
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {
    init()
    blockClassDistribution(example.labelAt(0).toInt) += example.weight
    featureObservers.zipWithIndex.foreach {
      x => x._1.observeClass(example.labelAt(0).toInt, example.featureAt(x._2), example.weight)
    }
  }
  /**
   * Disable a feature with index
   *
   * @param fIndex the index of a feature
   * @return Unit
   */
  def disableFeature(fIndex: Int): Unit = {

  }

  /**
   * Returns whether a node is active.
   *
   * @return whether a node is active
   */
  override def isActive(): Boolean = true

  /**
   * Returns whether a node is pure, which means it only has examples belonged to one class
   */
  def isPure(): Boolean = {
    this.classDistribution.filter(_ > 0).length <= 1 &&
      this.blockClassDistribution.filter(_ > 0).length <= 1
  }

  def weight(): Double = { classDistribution.sum + blockClassDistribution.sum }

  def blockWeight(): Double = blockClassDistribution.sum

  def addOnWeight(): Double = {
    if (blockWeight() != 0) blockWeight()
    else addonWeight
  }

  /**
   * merge the two node
   *
   * @param node the node which will be merged
   * @param trySplit whether to Hoeffding Tree is trying to split
   * @return current node
   */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (that.isInstanceOf[ActiveLearningNode]) {
      val node = that.asInstanceOf[ActiveLearningNode]
      //merge addonWeight and class distribution
      if (!trySplit) {
        this.addonWeight += that.blockClassDistribution.sum
        for (i <- 0 until blockClassDistribution.length)
          this.blockClassDistribution(i) += that.blockClassDistribution(i)
      } else {
        this.addonWeight = node.addonWeight
        for (i <- 0 until classDistribution.length)
          this.classDistribution(i) += that.blockClassDistribution(i)
      }
      //merge feature class observers
      for (i <- 0 until featureObservers.length)
        featureObservers(i) = featureObservers(i).merge(node.featureObservers(i), trySplit)
    }
    this
  }
  /**
   * Returns Split suggestions for all features, each is the best split for responding feature
   *
   * @param splitCriterion the SplitCriterion used
   * @param ht HoeffdingTreeModel
   * @return an array of FeatureSplit
   */
  def getBestSplitSuggestions(splitCriterion: SplitCriterion, ht: HoeffdingTreeModel): Array[FeatureSplit] = {
    val bestSplits = new ArrayBuffer[FeatureSplit]()
    featureObservers.zipWithIndex.foreach(x =>
      bestSplits.append(x._1.bestSplit(splitCriterion, classDistribution, x._2, ht.binaryOnly)))
    if (!ht.noPrePrune) {
      bestSplits.append(new FeatureSplit(null, splitCriterion.merit(classDistribution, Array.fill(1)(classDistribution)), new Array[Array[Double]](0)))
    }
    bestSplits.toArray
  }

  override def toString(): String = "level[" + dep + "]ActiveLearningNode:" + weight
}
/**
 * inactive learning node for hoeffding tree
 */
class InactiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  def this(that: InactiveLearningNode) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution))
  }

  /**
   * lean and update the node, for InactiveLearningNode, it just do nothing
   *
   * @param ht HoeffdingTreeModel
   * @param example an Example will be processed
   * @return Unit
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {}

  /**
   * Return whether a learning node is active
   *
   * @return whether a learning node is active
   */
  override def isActive(): Boolean = false

  /**
   * merge the two node
   *
   * @param node the node which will be merged
   * @param trySplit whether to split the current node
   * @return current node
   */
  override def merge(that: Node, trySplit: Boolean): Node = this

  override def toString(): String = "level[" + dep + "] InactiveLearningNode"
}
/**
 * class LearningNodeNB is a naive bayes learning node
 */
class LearningNodeNB(classDistribution: Array[Double], instanceSpecification: InstanceSpecification)
  extends ActiveLearningNode(classDistribution, instanceSpecification) with Serializable {

  def this(that: LearningNodeNB) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution), that.instanceSpecification)
    //init()
  }

  /**
   * Returns the predicted class distribution
   *
   * @param ht HoeffdingTreeModel
   * @param example  the Example to be evaluated
   * @return the predicted class distribution
   */
  override def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] = {
    if (weight() > ht.nbThreshold)
      NaiveBayes.predict(example, classDistribution, featureObservers)
    else super.classVotes(ht, example)
  }

  /**
   * Disable a feature with index
   *
   * @param fIndex the index of a feature
   * @return Unit
   */
  override def disableFeature(fIndex: Int): Unit = {
    featureObservers(fIndex) = new NullFeatureClassObserver()
  }
}

/**
 * class LearningNodeNBAdaptive is naive bayes adaptive learning node
 */

class LearningNodeNBAdaptive(classDistribution: Array[Double], instanceSpecification: InstanceSpecification)
  extends ActiveLearningNode(classDistribution, instanceSpecification) with Serializable {

  var mcCorrectWeight: Double = 0
  var nbCorrectWeight: Double = 0

  var mcBlockCorrectWeight: Double = 0
  var nbBlockCorrectWeight: Double = 0

  def this(that: LearningNodeNBAdaptive) {
    this(Util.mergeArray(that.classDistribution, that.blockClassDistribution), that.instanceSpecification)
    mcCorrectWeight = that.mcCorrectWeight + that.mcBlockCorrectWeight
    nbCorrectWeight = that.nbCorrectWeight + that.nbBlockCorrectWeight
    init()
  }

  /*
   * lean and update the node
   */
  override def learn(ht: HoeffdingTreeModel, example: Example): Unit = {
    super.learn(ht, example)
    if (Util.argmax(classDistribution) == example.labelAt(0)) mcBlockCorrectWeight += example.weight
    if (Util.argmax(NaiveBayes.predict(example, classDistribution, featureObservers)) == example.labelAt(0))
      nbBlockCorrectWeight += example.weight
  }

  /**
   * merge the two node
   *
   * @param node the node which will be merged
   * @param trySplit whether the Hoeffding Tree is trying to split
   * @return current node
   */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (that.isInstanceOf[LearningNodeNBAdaptive]) {
      val nbaNode = that.asInstanceOf[LearningNodeNBAdaptive]
      //merge weights and class distribution
      if (!trySplit) {
        this.addonWeight += nbaNode.blockClassDistribution.sum
        mcCorrectWeight += nbaNode.mcBlockCorrectWeight
        nbCorrectWeight += nbaNode.nbBlockCorrectWeight
        for (i <- 0 until blockClassDistribution.length)
          this.blockClassDistribution(i) += that.blockClassDistribution(i)
      } else {
        this.addonWeight = nbaNode.addonWeight
        mcCorrectWeight += nbaNode.mcCorrectWeight
        nbCorrectWeight += nbaNode.nbCorrectWeight
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
   * @param ht HoeffdingTreeModel
   * @param example the Example to be evaluated
   * @return the predicted class distribution
   */
  override def classVotes(ht: HoeffdingTreeModel, example: Example): Array[Double] = {
    if (mcCorrectWeight > nbCorrectWeight) super.classVotes(ht, example)
    else NaiveBayes.predict(example, classDistribution, featureObservers)
  }
}
