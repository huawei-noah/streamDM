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
import scala.io.Source

/*
 * A TreeCoreset contains the underlying tree structure for the coreset extraction
 * framework, it construct a tree structure for efficient coreset extraction from
 * a exmaple stream.
 */
class TreeCoreset {
  
  /*
   * Wrap the information in CoresetTreeNode, which will not be modified after
   * a CoresetTreeNode construction. It is composed of:
   * - the number of examples in a tree node
   * - the exmples associated with a tree node
   * - the cluster centre of the associated examples
   * - the parent of the tree node
   */
   class CoresetTreeElem(val n : Int, val points : Array[Example], 
    val centre : Example) {
    }

  /*
   * CoresetTreeNode data structure which is the component of a tree structure.
   * Each CoresetTreeNode is associated with a cluster of examples as well as some
   * other properties for the corresponding cluster. It is composed of:
   * - Wrapped properties of the coresetree node
   * - Left and right child of the node
   * - Cost of coresettree node
   */
  sealed trait CoresetTree {
    def elem : CoresetTreeElem
    def cost : Double
  }

  case class CoresetTreeLeaf(val elem : CoresetTreeElem, val cost : Double) extends CoresetTree {
    /*
     * Compute the distance between the given point and the leaf centre
     * @param point the given point
     * @return the distance between the point and the leaf centre
     */
    def costOfPoint(point : Example) : Double = {
      val weight = point.weight
      val instance = if(weight != 0.0) point.in.map(x=>x/weight) else point.in
      val centre = if(elem.centre.weight != 0.0) elem.centre.in.map(x=>x/elem.centre.weight) else elem.centre.in
      instance.distanceTo(centre)*weight
    }

    /*
     * Compute the leaf node cost attribute, which is the sum of the squared distances over
     * all points associated with the leaf node to its centre
     * @return a new leaf node with the computed cost value
     */
    def weightedLeaf() : CoresetTreeLeaf = {
      val points = elem.points
      new CoresetTreeLeaf(elem, points.map(point=>costOfPoint(point)).sum)
    }

    /*
     * Select a new centre from the leaf node for spliting. 
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


  case class CoresetTreeNode(val elem : CoresetTreeElem, val left : CoresetTree, 
    val right : CoresetTree, val cost : Double) extends CoresetTree {
  }

  /*
   * Split the coreset tree leaf to generate a coreset tree node with two leaves
   * @param leaf is the coreset tree leaf for spliting
   * @return a coreset tree node with two leaves
   */
  private def splitCoresetTreeLeaf(leaf : CoresetTreeLeaf) : CoresetTreeNode = {
    // Select a example from the points associated with the leaf  as a new centre for one of the new leaf 
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

  /*
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
  
  def retrieveCoreset(root :  CoresetTree, coreset : Array[Example]) : Array[Example] = root match {
    case CoresetTreeNode(e, l, r, c) => {
      retrieveCoreset(r, retrieveCoreset(l, coreset))
    }
    case CoresetTreeLeaf(e, c) => coreset :+ e.centre.setWeight(e.n)
  }

  /*
   * Build a coreset tree with the points and the coreset size
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

  /*
   * A help function for computing the squared distance between two examples
   * @param p1
   * @param p2
   * @return the squared distance 
   */
  def squaredDistance(p1 : Example, p2 : Example) : Double = {
    val inst1 = if(p1.weight != 0.0) p1.in.map(x=>x/p1.weight) else p1.in
    val inst2 = if(p2.weight != 0.0) p2.in.map(x=>x/p2.weight) else p2.in
    inst1.distanceTo(inst2)
  }
}

object TestTreeCoreset {
  
  private def printPoints(input: Array[Example]): Unit =
    input.foreach{ case x => println(x) }
  
  def main(args: Array[String]) {
    val points = Array(
    new Example(DenseInstance(Array(10.8348626966492, 18.7800980127523))),
    new Example(DenseInstance(Array(10.259545802461, 23.4515683763173))),
    new Example(DenseInstance(Array(11.7396623802245, 17.7026240456956))),
    new Example(DenseInstance(Array(12.4277617893575, 19.4887691804508))),
    new Example(DenseInstance(Array(10.1057940183815, 18.7332929859685))),
    new Example(DenseInstance(Array(11.0118378554584, 20.9773232834654))),
    new Example(DenseInstance(Array(7.03171204763376, 19.1985058633283))),
    new Example(DenseInstance(Array(6.56491029696013, 21.5098251711267))),
    new Example(DenseInstance(Array(10.7751248849735, 22.1517666115673))),
    new Example(DenseInstance(Array(8.90149362263775, 19.6314465074203))),
    new Example(DenseInstance(Array(11.931275122466, 18.0462702532436))),
    new Example(DenseInstance(Array(11.7265904596619, 16.9636039793709))),
    new Example(DenseInstance(Array(11.7493214262468, 17.8517235677469))),
    new Example(DenseInstance(Array(12.4353462881232, 19.6310467981989))),
    new Example(DenseInstance(Array(13.0838514816799, 20.3398794353494))),
    new Example(DenseInstance(Array(7.7875624720831, 20.1569764307574))),
    new Example(DenseInstance(Array(11.9096128931784, 21.1855674228972))),
    new Example(DenseInstance(Array(8.87507602702847, 21.4823134390704))),
    new Example(DenseInstance(Array(7.91362116378194, 21.325928219919))),
    new Example(DenseInstance(Array(26.4748241341303, 9.25128245838802))),
    new Example(DenseInstance(Array(26.2100410238973, 5.06220487544192))),
    new Example(DenseInstance(Array(28.1587146197594, 3.70625885635717))),
    new Example(DenseInstance(Array(26.8942422516129, 5.02646862012427))),
    new Example(DenseInstance(Array(23.7770902983858, 7.19445492687232))),
    new Example(DenseInstance(Array(23.6587920739353, 3.35476798095758))),
    new Example(DenseInstance(Array(23.7722765903534, 3.74873642284525))),
    new Example(DenseInstance(Array(23.9177161897547, 8.1377950229489))),
    new Example(DenseInstance(Array(22.4668345067162, 8.9705504626857))),
    new Example(DenseInstance(Array(24.5349708443852, 5.00561881333415))),
    new Example(DenseInstance(Array(24.3793349065557, 4.59761596097384))),
    new Example(DenseInstance(Array(27.0339042858296, 4.4151109960116))),
    new Example(DenseInstance(Array(21.8031183153743, 5.69297814349064))),
    new Example(DenseInstance(Array(22.636600400773, 2.46561420928429))),
    new Example(DenseInstance(Array(25.1439536911272, 3.58469981317611))),
    new Example(DenseInstance(Array(21.4930923464916, 3.28999356823389))),
    new Example(DenseInstance(Array(23.5359486724204, 4.07290025106778))),
    new Example(DenseInstance(Array(22.5447925324242, 2.99485404382734))),
    new Example(DenseInstance(Array(25.4645673159779, 7.54703465191098))))
    
    println("Training examples")
    printPoints(points)
    
    val treecoreset = new TreeCoreset
    val tree = treecoreset.buildCoresetTree(points, 8)
    val coreset = treecoreset.retrieveCoreset(tree, new Array[Example](0))
    println("Coreset examples")
    printPoints(coreset)
  }
  
}
