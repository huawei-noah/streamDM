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

import org.apache.spark.streamdm.classifiers.trees.HoeffdingTreeModel
import org.apache.spark.streamdm.core.Example

/**
  * Learning node class type for Hoeffding trees.
  */
abstract class LearningNode(classDistribution: Array[Double]) extends Node(classDistribution)
  with Serializable {

  /**
    * Learn and update the node
    *
    * @param ht a Hoeffding tree model
    * @param example the input Example
    */
  def learn(ht: HoeffdingTreeModel, example: Example): Unit

  /**
    * Return whether a learning node is active
    */
  def isActive(): Boolean

  /**
    * Filter the data to the related leaf node
    *
    * @param example the input example
    * @param parent the parent of current node
    * @param index the index of current node in the parent children
    * @return FoundNode containing the leaf node
    */
  override def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode =
    new FoundNode(this, parent, index)

}
