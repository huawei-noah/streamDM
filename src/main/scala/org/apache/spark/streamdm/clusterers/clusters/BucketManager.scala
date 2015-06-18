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
import scala.math
import scala.collection.mutable.Queue
import scala.util.control.Breaks._


/**
 * Data structure for managing all buckets for streamKM++ algorithm, this data
 * structure will extract maxsize coreset exmaples from n instances. When a new 
 * example comming, the bucketmanager will update buckets in it.
 * -n instances in DStream
 * -maxsize bucket size for coreset extraction
 */
class BucketManager(val n : Int, val maxsize : Int) extends Clusters {

  type T = BucketManager
  
  /** 
   * Inner class Bucket for new instance management, this class has two buffers for
   * recursively computation the coreset
   */
  class Bucket(val bucketsize : Int = maxsize) {
    val points = Queue[Example]()
    val spillover = Queue[Example]()
    def isFull : Boolean = if(points.length == bucketsize) true else false
  }
 
  val L = (math.ceil(math.log(n.toDouble/maxsize.toDouble)/math.log(2))+2).toInt
  val buckets = new Array[Bucket](L)
  for(i <- 0 until L)
    buckets(i) = new Bucket

  /** Update the clustering data structure, depending on the example given
   *
   * @param the exmaple based on which the Model is updated
   * @return the updated BucketManger object
   */
  override def update(change: Example): BucketManager = {
    // Check if there is enough space in the first bucket
    if(buckets(0).isFull){
      var curbucket : Int = 0
      var nextbucket : Int =1
      // Check if the next bucket is empty
      if(!buckets(nextbucket).isFull) {
        // Copy curbucket points to nextbucket points  
        val backpoints = buckets(curbucket).points.clone()
        for(point <- backpoints) buckets(nextbucket).points.enqueue(point)
        // Clear curbucket to empty
        buckets(curbucket).points.clear()
      } else {
        // Copy curbucket points to nextbucket spillover and continue 
        val backspillover = buckets(curbucket).points.clone()
        buckets(nextbucket).spillover.clear()
        for(point <- backspillover) buckets(nextbucket).spillover.enqueue(point)
        // Clear curbucket to empty
        buckets(curbucket).points.clear()
        curbucket += 1
        nextbucket += 1
        /*
         * As long as the nextbucket is full, output the coreset to the spillover of the next bucket
         */
        while(buckets(nextbucket).isFull) {
          val examples = (buckets(curbucket).points union buckets(curbucket).spillover).toArray 
          val tree = new TreeCoreset
          val coreset = tree.retrieveCoreset(tree.buildCoresetTree(examples, maxsize), new Array[Example](0))
          // Copy coreset to nextbucket spillover
          buckets(nextbucket).spillover.clear()
          for(point <- coreset) buckets(nextbucket).spillover.enqueue(point)
          // Clear curbucket
          buckets(curbucket).points.clear()
          buckets(curbucket).spillover.clear()
          curbucket += 1
          nextbucket += 1
        }
        val examples = (buckets(curbucket).points union buckets(curbucket).spillover).toArray 
        val tree = new TreeCoreset
        val coreset = tree.retrieveCoreset(tree.buildCoresetTree(examples, maxsize), new Array[Example](0))
        // Copy coreset to nextbucket points
        buckets(nextbucket).points.clear()
        for(point <- coreset) buckets(nextbucket).points.enqueue(point)
        // Clear curbucket
        buckets(curbucket).points.clear()
        buckets(curbucket).spillover.clear()
      }
    }
    
    buckets(0).points.enqueue(change)
    this
  }

  /**
   * Return an array of weighted examples corresponding to the coreset extracted from
   * treecoreset data structure Kmeans clustering
   * Case 1 : when the last bucket is full, return the contents of the last bucket
   * Case 2 : when the last bucket is not full, recursively compute a coreset from all
   *          nonempty buckets
   * 
   * @return coreset for the examples entered into the buckets.
   */
  def getCoreset: Array[Example] = {
    if(buckets(L-1).isFull) {
     buckets(L-1).points.toArray 
    }else {
      var i = 0
      var coreset = Array[Example]()
      for(i <- 0 until L) {
        if(buckets(i).isFull) {
          coreset = buckets(i).points.toArray
          break
        }
      }
      val start = i+1
      for(j <- start until L) {
        val examples = buckets(j).points.toArray ++ coreset
        val tree = new TreeCoreset
        coreset = tree.retrieveCoreset(tree.buildCoresetTree(examples, maxsize), new Array[Example](0))
      }
      coreset
    }
  }
}


object TestBucketManager {
  private def printPoints(input: Array[Example]): Unit =
    input.foreach{ case x => println(x) }
  
  def main(args: Array[String]) {
    var points = Array(
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
    
    // printPoints(points)

    var bucketmgt = new BucketManager(28, 4)
    for(i <- 0 until 28) {
      bucketmgt = bucketmgt.update(points(i))
      for(j <- 0 until 5) {
        println(j+"-th points buffer")
        printPoints(bucketmgt.buckets(j).points.toArray)
        println(j+"-th spillover buffer")
        printPoints(bucketmgt.buckets(j).spillover.toArray)
      }
    }
  }
}
