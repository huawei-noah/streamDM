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

package org.apache.spark.streamdm.clusterers

import org.apache.spark.streamdm.clusterers.clusters._
import org.apache.spark.streamdm.clusterers.utils._
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._
import com.github.javacliparser._
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * Implements the StreamKM++ algorithm for data streams. StreamKM++ computes a
 * small (weighted) sample of the stream by using <i>coresets</i>, and then uses
 * it as an input to a k-means++ algorithm. It uses a data structure called
 * <tt>BucketManager</tt> to handle the coresets.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Number of microclusters (<b>-m</b>)
 *  <li> Initial buffer size (<b>-b</b>)
 *  <li> Size of coresets (<b>-s</b>)
 *  <li> Learning window (<b>-w</b>) * </ul>
 */
class StreamKM extends Clusterer {
  
  type T = BucketManager

  var bucketmanager: BucketManager = null
  var numInstances: Long = 0
  var initialBuffer: Array[Example] = Array[Example]()
  
  val kOption: IntOption = new IntOption("numClusters", 'k',
    "Number of clusters for output", 10, 1, Integer.MAX_VALUE)
  
  val repOption: IntOption = new IntOption("kMeansIters", 'i',
    "Number of k-means iterations", 1000, 1, Integer.MAX_VALUE)

  val sizeCoresetOption: IntOption = new IntOption("sizeCoreset", 's',
    "Size of coreset", 10000, 1, Integer.MAX_VALUE)
  
  val widthOption: IntOption = new IntOption("width",
      'w', "Size of window for training learner.", 100000, 1, Integer.MAX_VALUE);
  
  var exampleLearnerSpecification: ExampleSpecification = null
  
  /** 
   * Init the StreamKM++ algorithm.
   */
  def init(exampleSpecification: ExampleSpecification) : Unit = {
    exampleLearnerSpecification = exampleSpecification
    bucketmanager = new BucketManager(widthOption.getValue, sizeCoresetOption.getValue)
  }
  
  /** 
   *  Maintain the BucketManager for coreset extraction, given an input DStream of Example.
   * @param input a stream of instances
   */
  def train(input: DStream[Example]): Unit = {
    input.foreachRDD(rdd => {
      rdd.foreach(ex => {
        bucketmanager = bucketmanager.update(ex)
        numInstances += 1
      })
    })
  }
  
  /**
   *  Gets the current Model used for the Learner.
   * @return the Model object used for training
   */
  def getModel: BucketManager = bucketmanager
  
  /** 
   * Get the currently computed clusters
   * @return an Array of Examples representing the clusters
   */
  def getClusters: Array[Example] = {
    if(numInstances <= sizeCoresetOption.getValue) {
      bucketmanager.buckets(0).points.toArray
    } 
    else {
     val streamingCoreset = bucketmanager.getCoreset
     KMeans.cluster(streamingCoreset, kOption.getValue, repOption.getValue)  
    }
  }
  
  /**
   *  Assigns examples to clusters, given the current Clusters data structure. 
   * @param input the DStream of Examples to be assigned a cluster
   * @return a DStream of tuples containing the original Example and the
   * assigned cluster.
   */
  def assign(input: DStream[Example]): DStream[(Example,Double)] = {
    input.map(x => {
      val assignedCl = getClusters.foldLeft((0,Double.MaxValue,0))(
        (cl,centr) => {
          val dist = centr.in.distanceTo(x.in)
          if(dist<cl._2) ((cl._3,dist,cl._3+1))
          else ((cl._1,cl._2,cl._3+1))
        })._1
      (x,assignedCl)
    })
  }
}
