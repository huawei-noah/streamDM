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

/**
 *
 * Hoeffding Tree or VFDT.
 *
 *
 *
 * A Hoeffding tree is an incremental, anytime decision tree induction algorithm
 *
 * that is capable of learning from massive data streams, assuming that the
 *
 * distribution generating examples does not change over time. Hoeffding trees
 *
 * exploit the fact that a small sample can often be enough to choose an optimal
 *
 * splitting attribute. This idea is supported mathematically by the Hoeffding
 *
 * bound, which quantiﬁes the number of observations (in our case, examples)
 *
 * needed to estimate some statistics within a prescribed precision (in our
 *
 * case, the goodness of an attribute).</p> <p>A theoretically appealing feature
 *
 * of Hoeffding Trees not shared by other incremental decision tree learners is
 *
 * that it has sound guarantees of performance. Using the Hoeffding bound one
 *
 * can show that its output is asymptotically nearly identical to that of a
 *
 * non-incremental learner using inﬁnitely many examples. See for details:</p>
 *
 *
 *
 * <p>G. Hulten, L. Spencer, and P. Domingos. Mining time-changing data streams.
 *
 * In KDD’01, pages 97–106, San Francisco, CA, 2001. ACM Press.</p>
 *
 *
 *
 * <p>Parameters:</p> <ul> <li> -m : Maximum memory consumed by the tree</li>
 *
 * <li> -n : Numeric estimator to use : <ul> <li>Gaussian approximation
 *
 * evaluating 10 splitpoints</li> <li>Gaussian approximation evaluating 100
 *
 * splitpoints</li> <li>Greenwald-Khanna quantile summary with 10 tuples</li>
 *
 * <li>Greenwald-Khanna quantile summary with 100 tuples</li>
 *
 * <li>Greenwald-Khanna quantile summary with 1000 tuples</li> <li>VFML method
 *
 * with 10 bins</li> <li>VFML method with 100 bins</li> <li>VFML method with
 *
 * 1000 bins</li> <li>Exhaustive binary tree</li> </ul> </li> <li> -e : How many
 *
 * instances between memory consumption checks</li> <li> -g : The number of
 *
 * instances a leaf should observe between split attempts</li> <li> -s : Split
 *
 * criterion to use. Example : InfoGainSplitCriterion</li> <li> -c : The
 *
 * allowable error in split decision, values closer to 0 will take longer to
 *
 * decide</li> <li> -t : Threshold below which a split will be forced to break
 *
 * ties</li> <li> -b : Only allow binary splits</li> <li> -z : Stop growing as
 *
 * soon as memory limit is hit</li> <li> -r : Disable poor attributes</li> <li>
 *
 * -p : Disable pre-pruning</li>
 *
 *  <li> -l : Leaf prediction to use: MajorityClass (MC), Naive Bayes (NB) or NaiveBayes
 *
 * adaptive (NBAdaptive).</li>
 *
 *  <li> -q : The number of instances a leaf should observe before
 *
 * permitting Naive Bayes</li>
 *
 * </ul>
 *
 */

class HoeffdingTreeModel(
  val numClasses: Int, val numFeatures: Int, val range: Int,
  val FeatureTypes: Array[FeatureType], val numericObserverType: Int = 0,
  val splitCriterion: SplitCriterion = new InfoGainSplitCriterion(),
  var growthAllowed: Boolean = true, val binaryOnly: Boolean = false,
  val graceNum: Int = 200, val tieThreshold: Double = 0.05,
  val nbThreshold: Int = 0,
  val splitConfedence: Double = 0.0000001, val learningNodeType: Int = 0)
  extends Model with Serializable {

  type T = HoeffdingTreeModel

  var activeNodeCount: Int = 0
  var inactiveNodeCount: Int = 0
  var deactiveNodeCount: Int = 0
  var decisionNodeCount: Int = 0

  var numExamples: Int = 0

  val featureObservers: ArrayBuffer[FeatureClassObserver] = new ArrayBuffer[FeatureClassObserver]()

  var root: Node = null
  def init(): Unit = {

    FeatureTypes.zipWithIndex.foreach(x => featureObservers.append(
      FeatureClassObserver.createFeatureClassObserver(x._1, numClasses, range, x._2)))

    root = createLearningNode(learningNodeType, featureObservers.toArray, numClasses)
    activeNodeCount += 1
  }

  override def update(point: Example): HoeffdingTreeModel = {
    numExamples += 1
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
        println("yes,split,current data num:" + numExamples)
        val best: FeatureSplit = bestSuggestions.last
        if (best.conditionalTest == null) {
          println("deactivate")
          deactiveLearningNode(activeNode, parent, pIndex)
        } else {
          println("split")
          val splitNode: SplitNode = new SplitNode(activeNode.classDistribution, best.conditionalTest)

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
    case 0 => new ActiveLearningNode(classDistribution, featureObservers)
    case 1 => new LearningNodeNB(classDistribution, featureObservers)
    case 2 => new LearningNodeNBAdaptive(classDistribution, featureObservers)
    case _ => new ActiveLearningNode(classDistribution, featureObservers)
  }

  def createLearningNode(nodeType: Int, featureObservers: Array[FeatureClassObserver], numClasses: Int): LearningNode = nodeType match {
    case 0 => new ActiveLearningNode(new Array[Double](numClasses), featureObservers)
    case 1 => new LearningNodeNB(new Array[Double](numClasses), featureObservers)
    case 2 => new LearningNodeNBAdaptive(new Array[Double](numClasses), featureObservers)
    case _ => new ActiveLearningNode(new Array[Double](numClasses), featureObservers)
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