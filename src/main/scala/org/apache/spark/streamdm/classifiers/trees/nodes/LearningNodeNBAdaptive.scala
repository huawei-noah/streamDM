package org.apache.spark.streamdm.classifiers.trees.nodes

import org.apache.spark.streamdm.classifiers.bayes.NaiveBayes
import org.apache.spark.streamdm.classifiers.trees.{HoeffdingTreeModel, Utils}
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.core.specification.InstanceSpecification
import org.apache.spark.streamdm.utils.Utils.argmax
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

/**
  * Adaptive Naive Bayes learning node.
  * This learning node maintains two way of prediction:
  * - MajorityClass, represented by mcCorrectWeight
  * - nbCorrectWeight, represented by nbCorrectWeight
  * Those counters will be incremented respectively depending on which one provides a correct prediction.
  * If one of two counters is dominant, the Vote (Prediction) will be executed by that winner.
  * For example: mcCorrectWeight = 500, nbCorrectWeight = 550. The prediction will be made by NaiveBayesClassifier.
  * On the contrary, the prediction will be made by MajorityClass (super)
  */

class LearningNodeNBAdaptive(classDistribution: Array[Double],
                             instanceSpecification: InstanceSpecification, numSplitFeatures: Int)
  extends ActiveLearningNode(classDistribution, instanceSpecification, numSplitFeatures) with Serializable {

  var mcCorrectWeight: Double = 0
  var nbCorrectWeight: Double = 0

  var mcBlockCorrectWeight: Double = 0
  var nbBlockCorrectWeight: Double = 0

  def this(that: LearningNodeNBAdaptive) {
    this(Utils.addArrays(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification, that.numSplitFeatures)
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

    if (argmax(classDistribution) == example.labelAt(0))
      mcBlockCorrectWeight += example.weight
    if (argmax(NaiveBayes.predict(example, classDistribution, featureObservers)) ==
      example.labelAt(0))
      nbBlockCorrectWeight += example.weight
    super.learn(ht, example)
  }

  /**
    * Merge two nodes
    *
    * @param that the node which will be merged
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
        this.addonWeight = nbaNode.blockAddonWeight
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
