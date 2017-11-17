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

package org.apache.spark.streamdm.clusterers.clusters

import org.apache.spark.streamdm.core._

import scala.util.Random

/**
 * A TreeCoreset contains the underlying tree structure for the coreset
 * extraction framework. It constructs a tree structure for efficient coreset
 * extraction from an  Example stream.
 */
class TreeCoreset {
  
  /**
   * Wrap the information in CoresetTreeNode, which will not be modified after
   * a CoresetTreeNode construction. 
   *
   * <p>It is composed of:
   * <ul>
   *  <li> the number of examples in a tree node
   *  <li> the examples associated with a tree node
   *  <li> the cluster centre of the associated examples
   *  <li> the parent of the tree node
   * </ul>
   */
   class CoresetTreeElem(val n : Int, val points : Array[Example], 
    val centre : Example) {
    }

  sealed trait CoresetTree {
    def elem : CoresetTreeElem
    def cost : Double
  }

  
  /**
   * CoresetTreeLeaf node data structure. A leaf does not have any children.
   */
  case class CoresetTreeLeaf(val elem : CoresetTreeElem, val cost : Double) extends CoresetTree {
    /**
     * Compute the distance between the given point and the leaf centre.
     * @param point the given Example point
     * @return the distance between the point and the leaf centre
     */
    def costOfPoint(point : Example) : Double = {
      val weight = point.weight
      val instance = if(weight != 0.0) point.in.map(x=>x/weight) else point.in
      val centre = if(elem.centre.weight != 0.0) elem.centre.in.map(x=>x/elem.centre.weight) 
                    else elem.centre.in
      instance.distanceTo(centre)*weight
    }

    /**
     * Compute the leaf node cost attribute, which is the sum of the squared distances over
     * all points associated with the leaf node to its centre.
     *
     * @return a new leaf node with the computed cost value
     */
    def weightedLeaf() : CoresetTreeLeaf = {
      val points = elem.points
      new CoresetTreeLeaf(elem, points.map(point=>costOfPoint(point)).sum)
    }

    /**
     * Select a new centre from the leaf node for splitting. 
     * @return the new centre
     */
    def chooseCentre() : Example = {
      val funcost = this.weightedLeaf().cost
      val points = elem.points
      var sum = 0.0

      for(point <- points) {
        sum += costOfPoint(point)/funcost
        if(sum >= Random.nextDouble) 
          return point
      }
      elem.centre
    }
  }

  /**
   * CoresetTreeNode data structure, a component of the CoresetTree structure.
   * Each CoresetTreeNode is associated with a cluster of Example, and has its
   * properties stored.
   *
   * <p> It is composed of:
   * <ul>
   *  <li> wrapped properties of the node
   *  <li> left and right children of the node
   *  <li> the cost of the node
   * </ul>
   */
  case class CoresetTreeNode(val elem : CoresetTreeElem, val left : CoresetTree, 
    val right : CoresetTree, val cost : Double) extends CoresetTree {
  }

  /**
   * Split the coreset tree leaf to generate a coreset tree node with two
   * leaves.
   * 
   * @param leaf coreset tree leaf for spliting
   * @return a coreset tree node with two leaves
   */
  private def splitCoresetTreeLeaf(leaf : CoresetTreeLeaf) : CoresetTreeNode = {
    // Select a example from the points associated with the leaf  as a new centre 
    // for one of the new leaf 
    val newcentre = leaf.chooseCentre
    // The original centre as the other leaf centre
    val oldcentre = leaf.elem.centre
    // The points associated with the orignial leaf, the points will be assigned the new leaves
    val points = leaf.elem.points
    
    // Assign points to leftpoints and rightpoints
    var leftpoints = new Array[Example](0)
    var rightpoints = new Array[Example](0)
    for(point <- points) {
      if(squaredDistance(point, newcentre) < squaredDistance(point, oldcentre))
        leftpoints = leftpoints :+ point
      else
        rightpoints = rightpoints :+ point
    }
    
    // Create new leaves 
    val leftElem = new CoresetTreeElem(leftpoints.length, leftpoints, newcentre)
    val leftleaf = CoresetTreeLeaf(leftElem, 0.0).weightedLeaf
    
    val rightElem = new CoresetTreeElem(rightpoints.length, rightpoints, oldcentre)
    val rightleaf = CoresetTreeLeaf(rightElem, 0.0).weightedLeaf
    
    // Return a coreset tree node with two leaves
    new CoresetTreeNode(leaf.elem, leftleaf, rightleaf, leftleaf.cost+rightleaf.cost)
  }

  /**
   * Split a coreset tree to construct a new coreset tree with recursive schemes
   * @param input the coreset tree root
   * @return the new coreset tree
   */
  private def splitCoresetTree(root : CoresetTree) : CoresetTree = root match {
    case CoresetTreeLeaf(e, c) => {
      splitCoresetTreeLeaf(CoresetTreeLeaf(e, c))
    }
    case CoresetTreeNode(e, l, r, c) => {
      if (Random.nextDouble > 0.5) {
        val lchild = splitCoresetTree(l)
        val newcost = lchild.cost + r.cost
        CoresetTreeNode(e, lchild, r, newcost)
      }
      else {
        val rchild = splitCoresetTree(r)
        val newcost = l.cost + rchild.cost
        CoresetTreeNode(e, l, rchild, newcost)
      }
    }
  }
  
  /**
   * Retrieve a new coreset from a given CoresetTree recursively. During this
   * process, the returned coreset will be combined with the given coreset.
   * @param root the CoresetTree root node
   * @param coreset the current coreset
   * @return the new coreset
   */
  def retrieveCoreset(root :  CoresetTree, coreset : Array[Example]) : Array[Example] = 
    root match {
      case CoresetTreeNode(e, l, r, c) => {
        retrieveCoreset(r, retrieveCoreset(l, coreset))
      }
      case CoresetTreeLeaf(e, c) => coreset :+ e.centre.setWeight(e.n)
  }

  /**
   * Build a coreset tree with the points and having the given coreset size
   * @param points for coreset tree construction
   * @param m is the coreset size
   * @return a coreset tree
   */
  def buildCoresetTree(points : Array[Example], m : Int) : CoresetTree = {
    // Random select a point from points as the initial centre for the tree root
    val initcentre = points(Random.nextInt(points.length))
    val elem = new CoresetTreeElem(points.length, points, initcentre)
    
    var root : CoresetTree = new CoresetTreeLeaf(elem, 0.0) 
    for(i <- 1 to m-1) {
      root = splitCoresetTree(root)
    }
    root
  }

  /**
   * A help function for computing the squared distance between two examples
   * @param p1 the source Example
   * @param p2 the target Example
   * @return the squared distance 
   */
  def squaredDistance(p1 : Example, p2 : Example) : Double = {
    val inst1 = if(p1.weight != 0.0) p1.in.map(x=>x/p1.weight) else p1.in
    val inst2 = if(p2.weight != 0.0) p2.in.map(x=>x/p2.weight) else p2.in
    inst1.distanceTo(inst2)
  }
}
