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

package org.apache.spark.streamdm.classifiers.trees.nodes

import org.apache.spark.streamdm.classifiers.bayes.NaiveBayes
import org.apache.spark.streamdm.classifiers.trees.{HoeffdingTreeModel, NullFeatureClassObserver, Utils}
import org.apache.spark.streamdm.core.Example
import org.apache.spark.streamdm.core.specification.InstanceSpecification

/**
  * Naive Bayes based learning node.
  */
class LearningNodeNB(classDistribution: Array[Double], instanceSpecification: InstanceSpecification,
                     numSplitFeatures: Int)
  extends ActiveLearningNode(classDistribution, instanceSpecification, numSplitFeatures) with Serializable {

  def this(that: LearningNodeNB) {
    this(Utils.addArrays(that.classDistribution, that.blockClassDistribution),
      that.instanceSpecification, that.numSplitFeatures)
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