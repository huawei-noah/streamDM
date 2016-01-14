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
import org.apache.spark.rdd._
import com.github.javacliparser._
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * A Clusterer trait defines the needed operations for any implemented
 * clustering algorithm. It provides methods for clustering and for returning
 * the computed cluster.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Number of microclusters (<b>-m</b>)
 *  <li> Initial buffer size (<b>-b</b>)
 *  <li> Number of clusters (<b>-k</b>)
 *  <li> Iterations (<b>-i</b>), number of iterations of the k-means alforithm 
 * </ul>
 */
class Clustream extends Clusterer {

  type T = MicroClusters

  var microclusters: MicroClusters = null
  var initialBuffer: Array[Example] = null
  var clusters: Array[Example] = null
 
  val kOption: IntOption = new IntOption("numClusters", 'k',
    "Number of clusters for output", 10, 1, Integer.MAX_VALUE)

  val mcOption: IntOption = new IntOption("numMicroclusters", 'm',
    "Size of microcluster buffer", 100, 1, Integer.MAX_VALUE)

  val initOption: IntOption = new IntOption("initialBuffer", 'b',
    "Size of initial buffer", 1000, 1, Integer.MAX_VALUE)

  val repOption: IntOption = new IntOption("kMeansIters", 'i',
    "Number of k-means iterations", 100, 1, Integer.MAX_VALUE)

  var exampleLearnerSpecification: ExampleSpecification = null

  /* Init the Clustream algorithm.
   *
   */
  def init(exampleSpecification: ExampleSpecification): Unit = {
    exampleLearnerSpecification = exampleSpecification
    microclusters = new MicroClusters(Array[MicroCluster]())
    initialBuffer = Array[Example]()
  }

  /* Maintain the micro-clusters, given an input DStream of Example.
   *
   * @param input a stream of instances
   */
  def train(input: DStream[Example]): Unit = {
    input.foreachRDD(rdd => {
      val numInstances: Long = initialBuffer.length + 1
      if (numInstances<initOption.getValue) {
        val neededInstances = (initOption.getValue - numInstances).toDouble
        val rddCount = rdd.count.toDouble
        var procRDD = rdd
        val fractionNeeded = neededInstances/rddCount
        val fractionRatio = 1.25
        //we are conservative: we get a bit more than we need
        if (fractionRatio*fractionNeeded<1.0) {
          procRDD = rdd.sample(false, fractionRatio*fractionNeeded)
        }
        initialBuffer = initialBuffer ++ ClusterUtils.fromRDDToArray(procRDD)
      }
      else if(microclusters.microclusters.length==0) {
        val timestamp = System.currentTimeMillis / 1000
        microclusters = new MicroClusters(Array.fill[MicroCluster]
                (mcOption.getValue)(new MicroCluster(new NullInstance(), 
                                    new NullInstance, 0, 0.0, 0)))
        //cluster the initial buffer to get the 
        //centroids of themicroclusters
        val centr = KMeans.cluster(initialBuffer, mcOption.getValue,
                                    repOption.getValue)
        //for every instance in the initial buffer, add it 
        //to the closest microcluster
        initialBuffer.foreach(iex => {
          val closest = ClusterUtils.assignToCluster(iex,centr)
          microclusters = microclusters.addToMicrocluster(closest, iex, 
                                                          timestamp)
        })
        microclusters = processMicroclusters(rdd, microclusters)
      }
      else {
        microclusters = processMicroclusters(rdd, microclusters)
      }
      //perform "offline" clustering
      if(initialBuffer.length<initOption.getValue) {
        clusters = KMeans.cluster(initialBuffer, kOption.getValue, 
                                  repOption.getValue)
      }
      else {
        val examples = microclusters.toExampleArray
        clusters = KMeans.cluster(examples, kOption.getValue,
                                  repOption.getValue)
      }
    })
  }

  /* Gets the current MicroClusters.
   * 
   * @return the current MicroClusters object
   */
  def getModel: MicroClusters = microclusters

  /**
   * Processes the new microclusters from an input RDD and given an initial
   * state of the microclusters.
   * @param rdd the input RDD of Example
   * @param microclusters the initial MicroClusters data structure
   * @return the updated microclusters
   */
  private def processMicroclusters(rdd: RDD[Example], input: MicroClusters):
    MicroClusters =
    rdd.aggregate(input)((mic,ex) =>mic.update(ex), (mic1,mic2) => mic1)



  /* Compute the output cluster centroids, based on the current microcluster
   * buffer; if no buffer is started, compute using k-means on the entire init
   * buffer.
   * @return an Array of Examples representing the clusters
   */
  def getClusters: Array[Example] =
    if (clusters==null) Array[Example]() else clusters

  
  /* Assigns examples to clusters, given the current microclusters. 
   *
   * @param input the DStream of Examples to be assigned a cluster
   * @return a DStream of tuples containing the original Example and the
   * assigned cluster.
   */
  def assign(input: DStream[Example]): DStream[(Example,Double)] = {
    input.map(x => {
      val assignedCl = ClusterUtils.assignToCluster(x,getClusters)
      (x,assignedCl)
    })
  }

}
