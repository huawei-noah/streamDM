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
 * structure will extract maxsize coreset exmaples from n instances.
 * -n instances in DStream
 * -maxsize bucket size for coreset extraction
 */
class BucketManager(val n : Int, val maxsize : Int) extends Clusters {

  type T = BucketManager
  
  /* Inner class Bucket for new instance management
   */
  class Bucket(val bucketsize : Int = maxsize) {
    val points = Queue[Example]()
    val spillover = Queue[Example]()
    def isFull : Boolean = if(points.length == bucketsize) true else false
  }
 
  val L = (math.ceil(math.log(n.toDouble/maxsize.toDouble)/math.log(2))+2).toInt
  val buckets = new Array[Bucket](L)

  /* Update the clustering data structure, depending on the example given
   *
   * @param the exmaple based on which the Model is updated
   * @return the updated BucketManger object
   */
  override def update(change: Instance): BucketManager = {
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
    
    buckets(0).points.enqueue(new Example(change))
    this
  }

  /*
   * Return an array of weighted examples corresponding to the coreset extracted from
   * treecoreset data structure Kmeans clustering
   * Case 1 : when the last bucket is full, return the contents of the last bucket
   * Case 2 : when the last bucket is not full, recursively compute a coreset from all
   *          nonempty buckets
   */
  def getCoreset: Array[Example] = {
    if(buckets(L-1).isFull) {
     buckets(L-1).points.toArray 
    }else {
      var i = 0
      var coreset = Array[Example]()
      for(i <- 0 to L) {
        if(buckets(i).isFull) {
          coreset = buckets(i).points.toArray
          break
        }
      }
      val start = i+1
      for(j <- start to L) {
        val examples = buckets(j).points.toArray ++ coreset
        val tree = new TreeCoreset
        coreset = tree.retrieveCoreset(tree.buildCoresetTree(examples, maxsize), new Array[Example](0))
      }
      coreset
    }
  }
}
