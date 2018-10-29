package org.apache.spark.streamdm.classifiers.trees.nodes

import org.apache.spark.streamdm.classifiers.trees._
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.core.specification.{FeatureSpecification, InstanceSpecification}

import scala.collection.mutable.ArrayBuffer

/**
  * Basic majority class active learning node for Hoeffding tree
  */
class ActiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  var addonWeight: Double = 0

  var blockAddonWeight: Double = 0

  var instanceSpecification: InstanceSpecification = null

  var featureObservers: Array[FeatureClassObserver] = null

  var numSplitFeatures: Int = -1

  def this(classDistribution: Array[Double], instanceSpecification: InstanceSpecification, numSplitFeatures: Int) {
    this(classDistribution)
    this.numSplitFeatures = numSplitFeatures
    this.instanceSpecification = instanceSpecification
    init()
  }

  def this(that: ActiveLearningNode) {
    this(Utils.addArrays(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification, that.numSplitFeatures)
    this.addonWeight = that.addonWeight
  }
  /**
    * init featureObservers array
    */
  def init(): Unit = {
    if (featureObservers == null) {

      val allFeatureIndexes = (0 to this.instanceSpecification.size()-1).toArray
      val selectedFeatureIndexes = Array.fill(this.numSplitFeatures)(-1)
      val randomSelector = (l: Array[Int]) => l(classifierRandom.nextInt(l.length))
      for(i <- 0 to selectedFeatureIndexes.length-1) {
        selectedFeatureIndexes(i) = randomSelector(allFeatureIndexes.filter(!selectedFeatureIndexes.contains(_)))
      }

      logInfo(selectedFeatureIndexes.mkString(" "))

      featureObservers = new Array(this.numSplitFeatures)

      for (i <- 0 until featureObservers.length) {
        val featureSpec: FeatureSpecification = instanceSpecification( selectedFeatureIndexes(i) )
        featureObservers(i) = FeatureClassObserver.createFeatureClassObserver(
          classDistribution.length, selectedFeatureIndexes(i), featureSpec)
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
    addonWeight += 1
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
    //not support yet
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

  def weight(): Double = { classDistribution.sum }

  def blockWeight(): Double = blockClassDistribution.sum

  def addOnWeight(): Double = {
    addonWeight
  }

  /**
    * Merge two nodes
    *
    * @param that the node which will be merged
    * @param trySplit flag indicating whether the node will be split
    * @return new node
    */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (that.isInstanceOf[ActiveLearningNode]) {
      val node = that.asInstanceOf[ActiveLearningNode]
      //merge addonWeight and class distribution
      if (!trySplit) {
        this.blockAddonWeight += node.blockClassDistribution.sum
        for (i <- 0 until blockClassDistribution.length)
          this.blockClassDistribution(i) += node.blockClassDistribution(i)
      } else {
        this.addonWeight = node.blockAddonWeight
        for (i <- 0 until classDistribution.length)
          this.classDistribution(i) += node.blockClassDistribution(i)
      }
      //merge feature class observers
      for (i <- 0 until featureObservers.length)
        featureObservers(i) = featureObservers(i).merge(node.featureObservers(i), trySplit)
    }
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
    featureObservers.zipWithIndex.foreach(x =>
      bestSplits.append(x._1.bestSplit(splitCriterion, classDistribution, x._2, ht.binaryOnly)))
    if (!ht.noPrePrune) {
      bestSplits.append(new FeatureSplit(null, splitCriterion.merit(classDistribution,
        Array.fill(1)(classDistribution)), new Array[Array[Double]](0)))
    }
    bestSplits.toArray
  }

  override def toString(): String = "level[" + dep + "]ActiveLearningNode:" + Utils.arraytoString(classDistribution)

  override def description(): String = {
    "[ActiveLearningNode] " + super.description()
  }

}