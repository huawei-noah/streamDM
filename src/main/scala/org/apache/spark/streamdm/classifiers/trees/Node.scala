package org.apache.spark.streamdm.classifiers.trees

import scala.collection.mutable.ArrayBuffer
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.util.Util._
class Node extends Serializable {
  var classDistribution: Array[Double] = null
  var level_ : Int = 1
  def this(classDistribution: Array[Double]) = {
    this()
    this.classDistribution = classDistribution
  }

  def setClassDistribution(classDistribution: Array[Double]): Node = {
    this.classDistribution = classDistribution
    this
  }
  def filterToLeaf(point: Example, parent: SplitNode, index: Int): FoundNode = new FoundNode(this, parent, index)
  def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = classDistribution
  def isLeaf() = true
  def level() = level_
  def setLevel(level: Int): Unit = { level_ = level }
  def sum():Int = 0
}

class FoundNode(val node: Node, val parent: SplitNode, val index: Int) extends Serializable {

}

class SplitNode extends Node with Serializable {
  var size: Int = 0
  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()
  var conditionalTest: ConditionalTest = null
  def this(classDistribution: Array[Double], conditionalTest: ConditionalTest, size: Int) {
    this()
    this.conditionalTest = conditionalTest
    this.classDistribution = classDistribution
    this.size = size
  }
  override def filterToLeaf(point: Example, parent: SplitNode, index: Int): FoundNode = {
    val cIndex = childIndex(point)
    if (cIndex >= 0) {
      if (cIndex < children.length && children(cIndex) != null)
        children(cIndex).filterToLeaf(point, this, cIndex)
      else new FoundNode(null, this, cIndex)
    } else new FoundNode(this, parent, index)
  }

  def childIndex(point: Example): Int = {
    conditionalTest.branch(point)
  }

  def setChild(index: Int, node: Node): Unit = {
    if (children.length > index) {
      children(index) = node
      node.setLevel(level_ + 1)
    } else if (children.length == index) {
      children.append(node)
      node.setLevel(level_ + 1)
    } else {
      assert(children.length < index)
    }
  }

  override def isLeaf() = false

  def numChildren(): Int = children.length
  
  override def sum():Int = {
    var sum = 0
    children.foreach { x => sum += x.sum}
    sum
  }

  override def toString(): String = {
    var head = "level[" + level_ + "]SplitNode\n"
    for (i <- 0 until (children.length)) {
      head += "Child[" + i + "]" + children(i).toString() + "\n"
    }
    head.substring(0, head.length()-1)
  }

}

abstract class LearningNode extends Node with Serializable {

  def learn(ht: HoeffdingTreeModel, point: Example): Unit = {
    //todo we may need extend classDistribution
    classDistribution(point.labelAt(0).toInt) += point.weight
  }
  def isActive(): Boolean = false
}

class ActiveLearningNode extends LearningNode with Serializable {

  var lastWeight_ : Double = 0.0
  var featureObservers: Array[FeatureClassObserver] = null

  def this(featureObservers: Array[FeatureClassObserver], numClasses: Int) {
    this()
    this.classDistribution = new Array[Double](numClasses)
    this.featureObservers = featureObservers
  }

  def this(featureObservers: Array[FeatureClassObserver], classDistribution: Array[Double]) {
    this()
    this.classDistribution = classDistribution
    this.featureObservers = featureObservers
    lastWeight_ = weight()
  }
  override def learn(ht: HoeffdingTreeModel, point: Example): Unit = {
    super.learn(ht, point)
    featureObservers.zipWithIndex.foreach {
      x => x._1.observeClass(point.labelAt(0).toInt, point.featureAt(x._2), point.weight)
    }
  }

  override def isActive(): Boolean = true

  def isPure(): Boolean = { this.classDistribution.filter(_ > 0).length <= 1 }

  def weight(): Double = classDistribution.sum
  def lastWeight(): Double = lastWeight_
  def setLastWeight(weight: Double): Unit = { lastWeight_ = weight }
  def getBestSplitSuggestions(splitCriterion: SplitCriterion, ht: HoeffdingTreeModel): Array[FeatureSplit] = {
    val bestSplits = new ArrayBuffer[FeatureSplit]()
    featureObservers.zipWithIndex.foreach(x =>
      bestSplits.append(x._1.bestSplit(splitCriterion, classDistribution, x._2, false)))
    bestSplits.toArray
  }

  override def sum():Int = weight.toInt
  override def toString(): String = "level[" + level_ + "]ActiveLearningNode:" + weight
}

class InactiveLearningNode extends LearningNode with Serializable {

  def this(numClasses: Int) {
    this()
    this.classDistribution = new Array[Double](numClasses)
  }

  def this(classDistribution: Array[Double]) {
    this()
    this.classDistribution = classDistribution
  }
  override def toString(): String = "level[" + level_ + "]InactiveLearningNode"
}

class LearningNodeNB extends ActiveLearningNode with Serializable {

  def this(featureObservers: Array[FeatureClassObserver], numClasses: Int) {
    this()
    this.classDistribution = new Array[Double](numClasses)
    this.featureObservers = featureObservers
  }

  def this(featureObservers: Array[FeatureClassObserver], classDistribution: Array[Double]) {
    this()
    this.classDistribution = classDistribution
    this.featureObservers = featureObservers
    lastWeight_ = weight()
  }

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = {
    if (weight > 0.01)
      NaiveBayes.predict(point, classDistribution, featureObservers.toArray)
    else super.classVotes(ht, point)
  }
}

class LearningNodeNBAdaptive extends ActiveLearningNode with Serializable {

  var mcCorrectWeight: Double = 0
  var nbCorrectWeight: Double = 0

  def this(featureObservers: Array[FeatureClassObserver], numClasses: Int) {
    this()
    this.classDistribution = new Array[Double](numClasses)
    this.featureObservers = featureObservers
  }

  def this(featureObservers: Array[FeatureClassObserver], classDistribution: Array[Double]) {
    this()
    this.classDistribution = classDistribution
    this.featureObservers = featureObservers
    lastWeight_ = weight()
  }

  override def learn(ht: HoeffdingTreeModel, point: Example): Unit = {
    super.learn(ht, point)
    if (argmax(classDistribution) == point.labelAt(0)) mcCorrectWeight += point.weight
    if (argmax(NaiveBayes.
      predict(point, classDistribution, featureObservers.toArray)) == point.labelAt(0))
      nbCorrectWeight += point.weight
  }

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = {
    if (mcCorrectWeight < nbCorrectWeight) null //todo
    else super.classVotes(ht, point)
  }
}
