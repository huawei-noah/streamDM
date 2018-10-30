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

import org.apache.spark.streamdm.classifiers.trees.{HoeffdingTreeModel, Utils}
import org.apache.spark.streamdm.core.Example

/**
  * Inactive learning node for Hoeffding trees
  */
class InactiveLearningNode(classDistribution: Array[Double])
  extends LearningNode(classDistribution) with Serializable {

  def this(that: InactiveLearningNode) {
    this(Utils.addArrays(that.classDistribution, that.blockClassDistribution))
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
    * @param that the node which will be merged
    * @param trySplit flag indicating whether the node will be split
    * @return new node
    */
  override def merge(that: Node, trySplit: Boolean): Node = this

  override def toString(): String = "level[" + dep + "] InactiveLearningNode"
}
