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

package org.apache.spark.streamdm.clusterers.utils

import org.apache.spark.streamdm.core._
import org.apache.spark.rdd._

import scala.util.Random
import scala.io.Source

/**
 * Clustering helper functions.
 */
object ClusterUtils extends Serializable {
  
  /**
   * Transforms an Example RDD into an array of RDD
   * @param input the input RDD
   * @return the output Array
   */
  def fromRDDToArray(input: RDD[Example]): Array[Example] =
    input.aggregate(Array[Example]())((arr, ex) => arr:+ex, 
                                    (arr1,arr2) => arr1++arr2)

  /**
   * Assigns the input example to the cluster corresponding to the closest
   * centroid.
   * @param example the input Example
   * @param centroids the Array of centroids
   * @return the assigned cluster index
   */
  def assignToCluster(example: Example, centroids: Array[Example]): Int =
    centroids.foldLeft((0,Double.MaxValue,0))((cl,centr) => {
      val dist = centr.in.distanceTo(example.in)
      if(dist<cl._2) ((cl._3,dist,cl._3+1))
      else ((cl._1,cl._2,cl._3+1))
    })._1
}

/**
 * The KMeans object computes the weighted k-means clustering given an array of
 * Examples. It assumes that the inputs are weighted. Each instance will
 * contribute a weight number of instances to the cluster.
 */
object KMeans extends Serializable {

  /**
   * Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   * @param input an Array of Example containing the instances to be clustered
   * @param k the number of clusters (default 10)
   * @param iterations the number of loops of k-means (default 1000)
   */
  def cluster(input: Array[Example], k: Int = 10, iterations: Int = 1000)
      : Array[Example] = {
    //sample k centroids from the input array
    //uses reservoir sampling to sample in one go
    var centroids = input.foldLeft((Array[Instance](),0))((a,e) => {
      if(a._2<k)
        (a._1:+e.in, a._2+1)
      else {
        var dice = Random.nextInt(a._2)
        if(dice<k) a._1(dice) = e.in
        (a._1, a._2+1)
      }
    })._1
    for(i <- 0 until iterations) {
      //initialize new empty clusters
      //each cluster will contain the sum of the instances in the cluster and
      //the number of instances
      var clusters: Array[(Instance,Double)] = 
                      Array.fill[(Instance,Double)](k)((new NullInstance,0.0))
      //find the closest centroid for each instance
      //assign the instance to the corresponding cluster
      input.foreach(ex => {
        val closest = ClusterUtils.assignToCluster(ex, centroids.map(
                        new Example(_)))
        clusters(closest) = addInstancesToCluster(clusters(closest),
                                                  (ex.in,ex.weight))
      })
      //recompute centroids
      centroids = clusters.foldLeft(Array[Instance]())((a,cl) => {
        val centroid = 
          if(cl._2==0) cl._1
          else cl._1.map(x => x/cl._2)
        a:+centroid
      })
    }
    centroids.map(new Example(_))
  }

  private def addInstancesToCluster(left: (Instance,Double), 
                                    right: (Instance,Double))
                                    : (Instance,Double) = 
    left._1 match {
      case NullInstance() => 
        (right._1.map(x=>x*right._2), right._2)
      case _ => 
        (left._1.add(right._1.map(x=>x*right._2)),left._2+right._2)
    }

}


/**
 * TestKmeans is used to test offline the k-means algorithm, and can be run via:
 * {{{
 *   sbt "run-main org.apache.spark.streamdm.clusterers.utils.TestKMeans
 *    <input_file> <k> <iterations> <instance_type=dense|sparse>"
 * }}}
 */
object TestKMeans {

  private def printCentroids(input: Array[Example]): Unit =
    input.foreach{ case x => println(x) }

  def main(args: Array[String]) {

    var data: Array[Example] = Array[Example]()

    for(line <- Source.fromFile(args(0)).getLines())
      data = data :+ Example.parse(line,args(3),"dense")

    var centroids = KMeans.cluster(data, args(1).toInt, args(2).toInt)

    printCentroids(centroids)

  }
}
