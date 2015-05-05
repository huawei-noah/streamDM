package org.apache.spark.streamdm.classifiers.trees

import scala.collection.mutable.ArrayBuffer
import scala.math.{ log, sqrt }
import org.apache.spark.streaming.dstream._
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers._

class HoeffdingTreeLearner extends Learner with Serializable {

  type T = HoeffdingTreeModel
  override def init(): Unit = {

  }
  var model: HoeffdingTreeModel = null
  override def getModel: HoeffdingTreeModel = model

  override def train(input: DStream[Example]): Unit = {

  }

  def predict(input: DStream[Example]): DStream[(Example, Double)] = { null }
}

class HoeffdingTreeModel(
  val numClasses: Int, val numFeatures: Int, val range: Int,
  val FeatureTypes: Array[FeatureType], val numericObserverType: Int = 0,
  val splitCriterion: SplitCriterion = new InfoGainSplitCriterion(),
  var growthAllowed: Boolean = true, val binaryOnly: Boolean = false,
  val graceNum: Int = 200, tieThreshold: Double = 0.05,
  val splitConfedence: Double = 0.0000001, val learningNodeType: Int = 0)
  extends Model with Serializable {

  type T = HoeffdingTreeModel

  var activeNodeCount: Int = 0
  var inactiveNodeCount: Int = 0
  var deactiveNodeCount: Int = 0
  var decisionNodeCount: Int = 0

  var examplenum: Int = 0

  val featureObservers: ArrayBuffer[FeatureClassObserver] = new ArrayBuffer[FeatureClassObserver]()

  {
    FeatureTypes.zipWithIndex.foreach(x => featureObservers.append(
      FeatureClassObserver.createFeatureClassObserver(x._1, numClasses, range, x._2)))
  }

  var root: Node = null
  def init(): Unit = {
    root = createLearningNode(learningNodeType, featureObservers.toArray, numClasses)
    activeNodeCount += 1
  }

  override def update(point: Example): HoeffdingTreeModel = {
    examplenum += 1
    if (root == null) {
      init
      println("init root")
    }
    val foundNode = root.filterToLeaf(point, null, -1)
    var leafNode = foundNode.node
    if (leafNode == null) {
      leafNode = createLearningNode(learningNodeType, featureObservers.toArray, numClasses)
      foundNode.parent.setChild(foundNode.index, leafNode)
      activeNodeCount += 1
      assert(false)
    }
    if (leafNode.isInstanceOf[LearningNode]) {
      val learnNode = leafNode.asInstanceOf[LearningNode]
      learnNode.learn(this, point)
      if (growthAllowed && learnNode.isInstanceOf[ActiveLearningNode]) {
        val activeNode = learnNode.asInstanceOf[ActiveLearningNode]
        if (activeNode.weight() - activeNode.lastWeight() >= graceNum) {
          attemptToSplit(activeNode, foundNode.parent, foundNode.index)
          // todo resize tree ONLY Split Nodes
          activeNode.setLastWeight(activeNode.weight())
        }
      }
      //println("learning")
    }
    this
  }

  def attemptToSplit(activeNode: ActiveLearningNode, parent: SplitNode, pIndex: Int): Unit = {
    if (!activeNode.isPure()) {
      var bestSuggestions: Array[FeatureSplit] = activeNode.getBestSplitSuggestions(splitCriterion, this)
      bestSuggestions = bestSuggestions.sorted
      if (shouldSplit(activeNode, bestSuggestions)) {
        println("yes,split,current data num:" + examplenum)
        val best: FeatureSplit = bestSuggestions.last
        if (best.conditionalTest == null) {
          println("deactivate")
          deactiveLearningNode(activeNode, parent, pIndex)
        } else {
          println("split")
          val splitNode: SplitNode = new SplitNode(activeNode.classDistribution, best.conditionalTest, best.numSplit)

          //addSplitNode(splitNode, parent, pIndex)
          for (index <- 0 until best.numSplit) {
            splitNode.setChild(index,
              createLearningNode(learningNodeType, featureObservers.toArray, best.distributionFromSplit(index)))
          }
          addSplitNode(splitNode, parent, pIndex)
          println("parent:" + parent + "at: " + pIndex)
        }
      }
      // todo manage memory
    }
  }
  def shouldSplit(activeNode: ActiveLearningNode, bestSuggestions: Array[FeatureSplit]): Boolean = {
    if (bestSuggestions.length < 2)
      bestSuggestions.length > 0
    else {
      val hoeffdingBound = computeHeoffdingBound(activeNode)
      val length = bestSuggestions.length

      if (hoeffdingBound < tieThreshold ||
        bestSuggestions.last.merit - bestSuggestions(length - 2).merit > hoeffdingBound) {
        print(bestSuggestions.last.merit - bestSuggestions(length - 2).merit + ",")
        println(hoeffdingBound)
        println((hoeffdingBound < tieThreshold) + "," + (
          bestSuggestions.last.merit - bestSuggestions(length - 2).merit > hoeffdingBound))
        true
      } else
        false
    }
  }
  def createLearningNode(nodeType: Int, featureObservers: Array[FeatureClassObserver], classDistribution: Array[Double]): LearningNode = nodeType match {
    case 0 => new ActiveLearningNode(featureObservers, classDistribution)
    case 1 => new LearningNodeNB(featureObservers, classDistribution)
    case 2 => new LearningNodeNBAdaptive(featureObservers, classDistribution)
    case _ => new ActiveLearningNode(featureObservers, classDistribution)
  }

  def createLearningNode(nodeType: Int, featureObservers: Array[FeatureClassObserver], numClasses: Int): LearningNode = nodeType match {
    case 0 => new ActiveLearningNode(featureObservers, numClasses)
    case 1 => new LearningNodeNB(featureObservers, numClasses)
    case 2 => new LearningNodeNBAdaptive(featureObservers, numClasses)
    case _ => new ActiveLearningNode(featureObservers, numClasses)
  }
  def activeLearningNode(inactiveNode: InactiveLearningNode, parent: SplitNode, pIndex: Int): Unit = {
    val activeNode = createLearningNode(learningNodeType, featureObservers.toArray, inactiveNode.classDistribution)
    if (parent == null) {
      root = activeNode
    } else {
      parent.setChild(pIndex, activeNode)
    }
    activeNodeCount += 1
    inactiveNodeCount -= 1
  }
  def deactiveLearningNode(activeNode: ActiveLearningNode, parent: SplitNode, pIndex: Int): Unit = {
    val deactiveNode = new InactiveLearningNode(activeNode.classDistribution)
    if (parent == null) {
      root = deactiveNode
    } else {
      parent.setChild(pIndex, deactiveNode)
    }
    activeNodeCount -= 1
    inactiveNodeCount += 1

  }
  def addSplitNode(splitNode: SplitNode, parent: SplitNode, pIndex: Int): Unit = {
    if (parent == null) {
      root = splitNode
    } else {
      parent.setChild(pIndex, splitNode)
    }
    activeNodeCount += splitNode.numChildren() - 1
    decisionNodeCount -= 1
  }
  def computeHeoffdingBound(activeNode: ActiveLearningNode): Double = {
    val rangeMerit = splitCriterion.rangeMerit(activeNode.classDistribution)
    sqrt(rangeMerit * rangeMerit * log(1.0 / this.splitConfedence)
      / (activeNode.weight() * 2))
  }
}