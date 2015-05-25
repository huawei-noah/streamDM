package org.apache.spark.streamdm.classifiers.trees

import scala.collection.mutable.ArrayBuffer
import scala.math.{ log, sqrt }

import com.github.javacliparser._

import org.apache.spark.streaming.dstream._
import org.apache.spark.streamdm.util.Util.{ argmax }
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers._

class HoeffdingTree extends Classifier {

  type T = HoeffdingTreeModel

  val runOnSparkOption: IntOption = new IntOption("runOnSpark", 'r',
    "run on Spark or not", 1, 0, 1)
  val numClassesOption: IntOption = new IntOption("numClasses", 'h',
    "Number of Classes", 2, 2, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 2, 1, Integer.MAX_VALUE)

  //  val featureTypesOption = new ClassOption("featureTypes", 'k',
  //    "feature type array.", classOf[FeatureTypeArray], "trees.FeatureTypeArray")

  val featureArray = new FeatureTypeArray(Array[FeatureType](new NominalFeatureType(10), new NominalFeatureType(10)))

  val numericObserverTypeOption: IntOption = new IntOption("numericObserverType", 'n',
    "numeric observer type, 0: gaussian", 0, 0, 2)

  //    val numericEstimatorOption = new ClassOption("numericEstimator",
  //            'n', "Numeric estimator to use.", classOf[FeatureClassObserver],
  //            "GuassianNumericFeatureClassOberser");

  val splitCriterionOption: ClassOption = new ClassOption("splitCriterion", 'c',
    "Split criterion to use.", classOf[SplitCriterion], "InfoGainSplitCriterion")

  //  val splitCriterion: SplitCriterion = splitCriterionOption.getValue()

  val growthAllowedOption: IntOption = new IntOption("growthAllowed", 'g',
    "Whether allow to grow", 1, 0, 1)

  val binaryOnlyOption: IntOption = new IntOption("binaryOnly", 'b',
    "Whether only allow binary splits", 0, 0, 1)

  val numGraceOption: IntOption = new IntOption("numGrace", 'm',
    "The number of instances a leaf should observe between split attempts.",
    200, 1, Int.MaxValue)

  val tieThresholdOption: FloatOption = new FloatOption("tieThreshold", 't',
    "Threshold below which a split will be forced to break ties.", 0.05, 0, 1)

  val splitConfidenceOption: FloatOption = new FloatOption("splitConfidence", 'd',
    "The allowable error in split decision, values closer to 0 will take longer to decide.",
    0.0000001, 0.0, 1.0)

  val learningNodeOption: IntOption = new IntOption("learningNodeType", 'o',
    "learning node type of leaf", 2, 0, 2)

  val nbThresholdOption: IntOption = new IntOption("nbThreshold", 'a',
    "naive bayes threshold", 0, 0, Int.MaxValue)

  val PrePruneOption: IntOption = new IntOption("PrePrune", 'p',
    "whether allow pre-pruning.", 0, 0, 1)

  /*
      public ClassOption numericEstimatorOption = new ClassOption("numericEstimator",
            'n', "Numeric estimator to use.", NumericAttributeClassObserver.class,
            "GaussianNumericAttributeClassObserver");

    public ClassOption nominalEstimatorOption = new ClassOption("nominalEstimator",
            'd', "Nominal estimator to use.", DiscreteAttributeClassObserver.class,
            "NominalAttributeClassObserver");

    public IntOption memoryEstimatePeriodOption = new IntOption(
            "memoryEstimatePeriod", 'e',
            "How many instances between memory consumption checks.", 1000000,
            0, Integer.MAX_VALUE);

    public FlagOption stopMemManagementOption = new FlagOption(
            "stopMemManagement", 'z',
            "Stop growing as soon as memory limit is hit.");

    public FlagOption removePoorAttsOption = new FlagOption("removePoorAtts",
            'r', "Disable poor attributes.");

    public FlagOption noPrePruneOption = new FlagOption("noPrePrune", 'p',
            "Disable pre-pruning.");
  
  
 */
  //  val numClasses: Int
  //  val numFeatures: Int
  //  val range: Int
  //  val FeatureTypes: Array[FeatureType]
  //  val numericObserverType: Int = 0
  //  val splitCriterion: SplitCriterion = new InfoGainSplitCriterion()
  //  var growthAllowed: Boolean = true
  //  val binaryOnly: Boolean = false
  //  val graceNum: Int = 200
  //  val tieThreshold: Double = 0.05
  //  val nbThreshold: Int = 0
  //  val splitConfedence: Double = 0.0000001
  //  val learningNodeType: Int = 0

  var model: HoeffdingTreeModel = null

  override def init(): Unit = {
    model = new HoeffdingTreeModel(runOnSparkOption.getValue() == 1,
      numClassesOption.getValue(), numFeaturesOption.getValue(),
      featureArray, numericObserverTypeOption.getValue, splitCriterionOption.getValue(),
      growthAllowedOption.getValue() == 1, binaryOnlyOption.getValue() == 1, numGraceOption.getValue(),
      tieThresholdOption.getValue, splitConfidenceOption.getValue(),
      learningNodeOption.getValue(), nbThresholdOption.getValue(),
      PrePruneOption.getValue() == 1)
  }

  override def getModel: HoeffdingTreeModel = model

  override def train(input: DStream[Example]): Unit = {
    input.foreachRDD {
      rdd =>
        val tmodel = rdd.aggregate(
          new HoeffdingTreeModel(model))(
            (mod, example) => { mod.update(example) }, (mod1, mod2) => mod1.blockMerge(mod2, false))
        model = tmodel
    }
  }

  def predict(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x => (x, model.predict(x)) }
  }
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

class HoeffdingTreeModel(val runOnSpark: Boolean,
                         val numClasses: Int, val numFeatures: Int,
                         val featureTypeArray: FeatureTypeArray, val numericObserverType: Int = 0,
                         val splitCriterion: SplitCriterion = new InfoGainSplitCriterion(),
                         var growthAllowed: Boolean = true, val binaryOnly: Boolean = false,
                         val graceNum: Int = 200, val tieThreshold: Double = 0.05,
                         val splitConfedence: Double = 0.0000001, val learningNodeType: Int = 0,
                         val nbThreshold: Int = 0, val prePrune: Boolean = false)
  extends Model with Serializable {

  type T = HoeffdingTreeModel

  var activeNodeCount: Int = 0
  var inactiveNodeCount: Int = 0
  var deactiveNodeCount: Int = 0
  var decisionNodeCount: Int = 0

  var baseNumExamples: Int = 0
  var blockNumExamples: Int = 0

  var lastExample: Example = null

  val featureObservers: Array[FeatureClassObserver] = new Array[FeatureClassObserver](numFeatures)

  var root: Node = null

  def this(model: HoeffdingTreeModel) {
    this(model.runOnSpark, model.numClasses, model.numFeatures, model.featureTypeArray,
      model.numericObserverType, model.splitCriterion, model.growthAllowed,
      model.binaryOnly, model.graceNum, model.tieThreshold, model.splitConfedence,
      model.learningNodeType, model.nbThreshold, model.prePrune)
    activeNodeCount = model.activeNodeCount
    this.inactiveNodeCount = model.inactiveNodeCount
    this.deactiveNodeCount = model.deactiveNodeCount
    this.decisionNodeCount = model.decisionNodeCount
    baseNumExamples = model.baseNumExamples + model.blockNumExamples
    this.root = model.root
    for (i <- 0 until featureObservers.length)
      featureObservers(i) = FeatureClassObserver.createFeatureClassObserver(model.featureObservers(i))
  }

  def init(): Unit = {

    featureTypeArray.featureTypes.zipWithIndex.foreach(x => featureObservers(x._2) =
      FeatureClassObserver.createFeatureClassObserver(x._1, numClasses, x._2, x._1.getRange()))

    root = createLearningNode(learningNodeType, numClasses)
    activeNodeCount += 1
  }

  override def update(point: Example): HoeffdingTreeModel = {
    blockNumExamples += 1
    if (root == null) {
      init
      println("init root")
    }
    val foundNode = root.filterToLeaf(point, null, -1)
    var leafNode = foundNode.node
    if (leafNode == null) {
      leafNode = createLearningNode(learningNodeType, numClasses)
      foundNode.parent.setChild(foundNode.index, leafNode)
      activeNodeCount += 1
      assert(false)
    }
    if (leafNode.isInstanceOf[LearningNode]) {
      val learnNode = leafNode.asInstanceOf[LearningNode]
      learnNode.learn(this, point)
      if (!runOnSpark)
        attemptToSplit(learnNode, foundNode.parent, foundNode.index)
      //      if (growthAllowed && learnNode.isInstanceOf[ActiveLearningNode]) {
      //        val activeNode = learnNode.asInstanceOf[ActiveLearningNode]
      //        if (activeNode.weight() - activeNode.lastWeight() >= graceNum) {
      //          attemptToSplit(activeNode, foundNode.parent, foundNode.index)
      //          // todo resize tree ONLY Split Nodes
      //          activeNode.setLastWeight(activeNode.weight())
      //        }
      //      }
      //println("learning")
    }
    this
  }

  def attemptToSplit(learnNode: LearningNode, parent: SplitNode, pIndex: Int): Unit = {
    if (growthAllowed && learnNode.isInstanceOf[ActiveLearningNode]) {
      val activeNode = learnNode.asInstanceOf[ActiveLearningNode]
      if (activeNode.addOnWeight() >= graceNum) {
        if (!activeNode.isPure()) {
          var bestSuggestions: Array[FeatureSplit] = activeNode.getBestSplitSuggestions(splitCriterion, this)
          bestSuggestions = bestSuggestions.sorted
          if (shouldSplit(activeNode, bestSuggestions)) {
            println("yes,split,current data num:" + baseNumExamples + blockNumExamples)
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
                  createLearningNode(learningNodeType, best.distributionFromSplit(index)))
              }
              addSplitNode(splitNode, parent, pIndex)
              println("parent:" + parent + "at: " + pIndex)
            }
          }
          // todo manage memory
        }
      }
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
  /*
   * merge with another model's FeatureObservers and try to split
   */
  def blockMerge(that: HoeffdingTreeModel, trySplit: Boolean): HoeffdingTreeModel = {
    for (i <- 0 until featureObservers.length)
      featureObservers(i) = featureObservers(i).blockMerge(that.featureObservers(i))
    if (trySplit) {
      if (root == null) {
        init
        println("init root in merge, shouldn't happen")
      }
      // merge node
      val foundNode = root.filterToLeaf(lastExample, null, -1)
      var leafNode = foundNode.node
      if (leafNode.isInstanceOf[LearningNode]) {
        val learnNode = leafNode.asInstanceOf[LearningNode]
        attemptToSplit(learnNode, foundNode.parent, foundNode.index)
      }
    }
    this
  }
  /* predict the class of point */
  def predict(point: Example): Double = {
    if (root != null) {
      val foundNode = root.filterToLeaf(point, null, -1)
      var leafNode = foundNode.node
      if (leafNode == null) {
        leafNode = foundNode.parent
      }
      argmax(leafNode.classVotes(this, point))
    } else 0.0
  }

  def createLearningNode(nodeType: Int, classDistribution: Array[Double]): LearningNode = nodeType match {
    case 0 => new ActiveLearningNode(classDistribution)
    case 1 => new LearningNodeNB(classDistribution)
    case 2 => new LearningNodeNBAdaptive(classDistribution)
    case _ => new ActiveLearningNode(classDistribution)
  }

  def createLearningNode(nodeType: Int, numClasses: Int): LearningNode = nodeType match {
    case 0 => new ActiveLearningNode(new Array[Double](numClasses))
    case 1 => new LearningNodeNB(new Array[Double](numClasses))
    case 2 => new LearningNodeNBAdaptive(new Array[Double](numClasses))
    case _ => new ActiveLearningNode(new Array[Double](numClasses))
  }
  def activeLearningNode(inactiveNode: InactiveLearningNode, parent: SplitNode, pIndex: Int): Unit = {
    val activeNode = createLearningNode(learningNodeType, inactiveNode.classDistribution)
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