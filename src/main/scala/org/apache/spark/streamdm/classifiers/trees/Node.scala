package org.apache.spark.streamdm.classifiers.trees

import scala.collection.mutable.ArrayBuffer
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.util.Util._

/**
 * trait Node for hoeffding Tree
 */
trait Node extends Serializable {

  var level_ : Int = 0

  def filterToLeaf(point: Example, parent: SplitNode, index: Int): FoundNode = new FoundNode(this, parent, index)

  def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double]

  def isLeaf() = true

  def level() = level_

  def setLevel(level: Int): Unit = {
    level_ = level
    if (this.isInstanceOf[SplitNode]) {
      val splidNode = this.asInstanceOf[SplitNode]
      splidNode.children.foreach { _.setLevel(level + 1) }
    }
  }

  def sum(): Int = 0
}

class FoundNode(val node: Node, val parent: SplitNode, val index: Int) extends Serializable {

}
/**
 * branch node for Hoeffding Tree
 */
class SplitNode(val classDistribution: Array[Double], val conditionalTest: ConditionalTest)
  extends Node with Serializable {

  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()

  override def filterToLeaf(point: Example, parent: SplitNode, index: Int): FoundNode = {
    val cIndex = childIndex(point)
    if (cIndex >= 0) {
      if (cIndex < children.length && children(cIndex) != null) {
        children(cIndex).filterToLeaf(point, this, cIndex)
      } else new FoundNode(null, this, cIndex)
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

  override def sum(): Int = {
    var sum = 0
    children.foreach { x => sum += x.sum }
    sum
  }

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = classDistribution

  override def toString(): String = {
    var head = "level[" + level_ + "]SplitNode\n"
    for (i <- 0 until (children.length)) {
      head += "Child[" + i + "]" + children(i).toString() + "\n"
    }
    head.substring(0, head.length() - 1)
  }

}
/**
 * trait learning node for Hoeffding Tree
 */
trait LearningNode extends Node with Serializable {

  def learn(ht: HoeffdingTreeModel, point: Example): Unit

  def isActive(): Boolean = false
}
/**
 * basic majority class active learning node for hoeffding tree
 */
class ActiveLearningNode(val classDistribution: Array[Double], val featureObservers: Array[FeatureClassObserver]) extends LearningNode with Serializable {

  var lastWeight_ : Double = weight()

  override def learn(ht: HoeffdingTreeModel, point: Example): Unit = {
    classDistribution(point.labelAt(0).toInt) += point.weight
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

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = classDistribution

  override def sum(): Int = weight.toInt

  override def toString(): String = "level[" + level_ + "]ActiveLearningNode:" + weight
}
/**
 * inactive learning node
 */
class InactiveLearningNode(val classDistribution: Array[Double]) extends LearningNode with Serializable {

  override def learn(ht: HoeffdingTreeModel, point: Example): Unit = {}

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = classDistribution

  override def toString(): String = "level[" + level_ + "]InactiveLearningNode"
}
/**
 * naive bayes learning node
 */
class LearningNodeNB(classDistribution: Array[Double], featureObservers: Array[FeatureClassObserver])
  extends ActiveLearningNode(classDistribution, featureObservers) with Serializable {

  override def classVotes(ht: HoeffdingTreeModel, point: Example): Array[Double] = {
    if (weight > 0.01) //todo
      NaiveBayes.predict(point, classDistribution, featureObservers.toArray)
    else super.classVotes(ht, point)
  }
}

/**
 * naive bayes adaptive learning node
 */

class LearningNodeNBAdaptive(classDistribution: Array[Double], featureObservers: Array[FeatureClassObserver])
  extends ActiveLearningNode(classDistribution, featureObservers) with Serializable {

  var mcCorrectWeight: Double = 0
  var nbCorrectWeight: Double = 0

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
