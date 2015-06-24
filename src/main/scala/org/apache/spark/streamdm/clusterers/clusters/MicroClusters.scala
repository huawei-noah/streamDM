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

import com.github.javacliparser._

import org.apache.spark.streamdm.core._

/**
 * The MicroClusters object contains a number of microclusters in an Array. It
 * provides operations for inserting, appending and removing microclusters.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Time horizon (<b>-h</b>)
 *  <li> Cluster radius multiplier (<b>-r</b>)
 *  <li> Value of m (<b>-m</b>), controlling number of standard deviations for
 *  expiry
 * </ul>
 */
case class MicroClusters(val microclusters: Array[MicroCluster]) 
  extends Clusters {

  type T = MicroClusters

  val horizonOption: IntOption = new IntOption("timeHorizon", 'h',
    "Size of horizon window (in seconds)", 1000, 1, Integer.MAX_VALUE)

  val radiusOption: FloatOption = new FloatOption("clusterRadius", 'r',
    "The kernel radius multiplier", 2.0, 1, Float.MaxValue)

  val mOption: IntOption = new IntOption("mValue", 'm', "Value of m",
    100, 1, Integer.MAX_VALUE)

  /**
   * Update the clustering data structure, depending on the Example given.
   *
   * @param inst the Example based on which the Model is updated
   * @return the updated MicroClusters object
   */
  override def update(change: Example): MicroClusters = {
    val timestamp = System.currentTimeMillis / 1000
    //Find the closest kernel to the instance
    val clTuple = microclusters.foldLeft((0,Double.MaxValue,0))((cl,mcl) => {
      val dist = change.in.distanceTo(mcl.centroid)
      if(dist<cl._2) ((cl._3,dist,cl._3+1))
       else ((cl._1,cl._2,cl._3+1))
    })
    val closest = clTuple._1
    val distance = clTuple._2
    //Compute the radius of the closest microcluster
    val radius = radiusOption.getValue * microclusters(closest).rmse
    if(distance<=radius) {
      //if within the radius, add it to the closest
      addToMicrocluster(closest,change,timestamp)
    }
    else {
      val mc = new MicroCluster(change.in.map(x=>x), change.in.map(x=>x*x),
                                timestamp, timestamp*timestamp, 1)
      //Find whether an expired microcluster exists
      val threshold = timestamp - horizonOption.getValue
      var i: Int = 0
      var found: Boolean = false
      var tmc: Int = 0
      while (i<microclusters.length && !found) {
        if(microclusters(i).threshold(mOption.getValue)<threshold) {
          found = true
          tmc = i
        }
        i += 1
      }
      if (found) {
        removeMicrocluster(tmc).appendMicrocluster(mc)
      }
      else {
        //find the two closest microclusters
        var sm: Int = 0
        var tm: Int = 0
        var dist: Double = Double.MaxValue
        var i: Int = 0
        var j: Int = 0
        while(i<microclusters.length) {
          j = i+1
          while(j<microclusters.length) {
            val dst = this.distance(i,j)
            if(dst<=dist) {
              sm = i
              tm = j
              dist = dst
            }
            j += 1
          }
          i += 1
        }
        if(sm!=tm)
          mergeMicroclusters(sm,tm).appendMicrocluster(mc)
        else
          new MicroClusters(microclusters)    
      }
    }
  }

  /**
   * Add an instance to the microcluster at a given instance. 
   *
   * @param index the index where the microcluster is
   * @param change the Example containing the changed instance
   * @return the updated MicroClusters object
   */
  def addToMicrocluster(index: Int, change: Example, timestamp:Long)
    : MicroClusters = {
    new MicroClusters(microclusters.updated(index,
                      microclusters(index).insert(change.in, timestamp)))
  }

  /**
   * Append a microcluster.
   *
   * @param mc the microcluster to be appended
   * @return the updated MicroClusters object
   */
  def appendMicrocluster(mc: MicroCluster): MicroClusters = {
    new MicroClusters(microclusters:+mc)
  }

  /**
   * Remove a microcluster from the buffer.
   *
   * @param index the index where the microcluster to be removed is
   * @return the updated MicroClusters object
   */
  private def removeMicrocluster(index: Int): MicroClusters =
    new MicroClusters(microclusters.take(index)++microclusters.drop(index+1))

  /**
   * Merge the source microcluster into the target microcluster.
   *
   * @param target the index of the target microcluster
   * @param source the index of the source microcluster
   * @return the updated MicroClusters object
   */
  private def mergeMicroclusters(target: Int, source: Int): MicroClusters = {
    if(target!=source) {
      var mergedMicroclusters = microclusters.updated(target,
                            microclusters(target).merge(microclusters(source)))
      new MicroClusters(mergedMicroclusters.take(source)++
                        mergedMicroclusters.drop(source+1))
    }
    else
      this
  }

  /**
   * Return the distance between two microclusters.
   *
   * @param source the index of the source microcluster
   * @param target the index of the target microcluster
   * @return the distane between microclusters 
   */
  private def distance(source: Int, target: Int): Double =
    microclusters(source).centroid.distanceTo(microclusters(target).centroid)

  /**
   * Return an array of weighted centroids corresponding to the microclusters.
   *
   * @return the output Example array
   */ 
  def toExampleArray: Array[Example] = microclusters.map(_.toExample)

  override def toString: String = {
    var str:String = ""
    var idx:Int = 0
    microclusters.foreach(mc => {
      str += "%d: %s\n".format(idx,mc.toString)
      idx += 1
    })
    str
  }
}
