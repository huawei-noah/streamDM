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

/**
 * A Clusterer trait defines the needed operations for any implemented
 * clustering algorithm. It provides methods for clustering and for returning
 * the computed cluster.
 */
class Clustream extends Clusterer {

  type T = MicroClusters

  var microclusters: MicroClusters = null
  var numInstances: Long = 0
  var initialBuffer: Array[Example] = Array[Example]()
 
  val kOption: IntOption = new IntOption("numClusters", 'k',
    "Number of clusters for output", 10, 1, Integer.MAX_VALUE)

  val mcOption: IntOption = new IntOption("numMicroclusters", 'm',
    "Size of microcluster buffer", 100, 1, Integer.MAX_VALUE)

  val initOption: IntOption = new IntOption("initialBuffer", 'b',
    "Size of initial buffer", 1000, 1, Integer.MAX_VALUE)

  val repOption: IntOption = new IntOption("kMeansIters", 'i',
    "Number of k-means iterations", 1000, 1, Integer.MAX_VALUE)

  /* Init the Clustream algorithm.
   *
   */
  def init: Unit = {
    microclusters = new MicroClusters(Array[MicroCluster]())
  }

  /* Maintain the micro-clusters, given an input DStream of Example.
   *
   * @param input a stream of instances
   */
  def train(input: DStream[Example]): Unit = {
    input.foreachRDD(rdd => {
      rdd.foreach(ex => {
        numInstances += 1
        if(numInstances<initOption.getValue){
          //the first elements are used in an initialization buffer
          initialBuffer = initialBuffer:+ex
        }
        else if(numInstances==initOption.getValue){
          val timestamp = System.currentTimeMillis / 1000
          initialBuffer = initialBuffer:+ex
          //initialize an empty microcluster buffer
          microclusters = new MicroClusters(Array.fill[MicroCluster]
            (mcOption.getValue)(new MicroCluster(new NullInstance(), 
                        new NullInstance, 0, 0.0, 0)))
          //cluster the initial buffer to get the centroids of the microclusters
          val centr = KMeans.cluster(initialBuffer, mcOption.getValue,
                                     repOption.getValue)
          //for every instance in the initial buffer, add it to the closest
          //microcluster
          initialBuffer.foreach(iex => {
            val closest = centr.foldLeft((0,Double.MaxValue,0))((cl,centr) => {
              val dist = centr.in.distanceTo(iex.in)
              if(dist<cl._2) ((cl._3,dist,cl._3+1))
              else ((cl._1,cl._2,cl._3+1))
            })._1
            microclusters = microclusters.addToMicrocluster(closest, iex, 
                                                            timestamp)
          })
        }
        else {
          //if not in initial step, maintain the microclusters 
          microclusters = microclusters.update(ex)
        }
      })
    })
  }

  /* Gets the current MicroClusters.
   * 
   * @return the current MicroClusters object
   */
  def getModel: MicroClusters = microclusters


  /* Compute the output cluster centroids, based on the current microcluster
   * buffer; if no buffer is started, compute using k-means on the entire init
   * buffer.
   * @return an Array of Examples representing the clusters
   */
  def getClusters: Array[Example] = {
    if(numInstances<initOption.getValue) {
      KMeans.cluster(initialBuffer, kOption.getValue, repOption.getValue)
    }
    else {
      val examples = microclusters.toExampleArray
      KMeans.cluster(examples, kOption.getValue, repOption.getValue)
    }
  }

}
