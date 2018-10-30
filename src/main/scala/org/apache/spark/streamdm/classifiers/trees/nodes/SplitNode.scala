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

import org.apache.spark.streamdm.classifiers.trees.{ConditionalTest, Utils}
import org.apache.spark.streamdm.core.Example

import scala.collection.mutable.ArrayBuffer
import scala.math.max

/**
  * Branch node of the Hoeffding tree.
  */
class SplitNode(classDistribution: Array[Double], val conditionalTest: ConditionalTest)
  extends Node(classDistribution) with Serializable {

  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()

  def this(that: SplitNode) {
    this(Utils.addArrays(that.classDistribution, that.blockClassDistribution),
      that.conditionalTest)
  }

  /**
    * Filter the data to the related leaf node
    *
    * @param example input example
    * @param parent the parent of current node
    * @param index the index of current node in the parent children
    * @return FoundNode cotaining the leaf node
    */
  override def filterToLeaf(example: Example, parent: SplitNode, index: Int): FoundNode = {
    val cIndex = childIndex(example)
    if (cIndex >= 0) {
      if (cIndex < children.length && children(cIndex) != null) {
        children(cIndex).filterToLeaf(example, this, cIndex)
      } else new FoundNode(null, this, cIndex)
    } else new FoundNode(this, parent, index)
  }

  def childIndex(example: Example): Int = {
    conditionalTest.branch(example)
  }

  def setChild(index: Int, node: Node): Unit = {
    if (children.length > index) {
      children(index) = node
      node.setDepth(dep + 1)
    } else if (children.length == index) {
      children.append(node)
      node.setDepth(dep + 1)
    } else {
      assert(children.length < index)
    }
  }
  /**
    * Returns whether a node is a leaf
    */
  override def isLeaf() = false

  /**
    * Returns height of the tree
    *
    * @return the height
    */
  override def height(): Int = {
    var height = 0
    for (child: Node <- children) {
      height = max(height, child.height()) + 1
    }
    height
  }

  /**
    * Returns number of children
    *
    * @return  number of children
    */
  override def numChildren(): Int = children.filter { _ != null }.length

  /**
    * Merge two nodes
    *
    * @param that the node which will be merged
    * @param trySplit flag indicating whether the node will be split
    * @return new node
    */
  override def merge(that: Node, trySplit: Boolean): Node = {
    if (!that.isInstanceOf[SplitNode]) this
    else {
      val splitNode = that.asInstanceOf[SplitNode]
      for (i <- 0 until children.length)
        this.children(i) = (this.children(i)).merge(splitNode.children(i), trySplit)
      this
    }
  }

  /**
    * Returns the node description
    * @return String containing the description
    */
  override def description(): String = {
    val sb = new StringBuffer("  " * dep + "\n")
    val testDes = conditionalTest.description()
    for (i <- 0 until children.length) {
      sb.append("  " * dep + " if " + testDes(i) + "\n")
      sb.append("  " * dep + children(i).description())
    }
    sb.toString()
  }

  override def toString(): String = "level[" + dep + "] SplitNode"

}
