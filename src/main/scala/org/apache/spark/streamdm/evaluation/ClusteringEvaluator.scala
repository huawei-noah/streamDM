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
package org.apache.spark.streamdm.evaluation

import math._

import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.rdd.RDD

/**
 * Clustering evaluator which computes, for each RDD of Example-Double tuples,
 * the clustering cohesion measure (SSE). This is the sum of square distances
 * between Examples and the centroid of their assigned cluster.
 */
class ClusteringCohesionEvaluator extends Evaluator {

  /**
   * Process the result of a predicted stream of Examples and Doubles.
   *
   * @param input the input stream containing (Example,Double) tuples
   * @return a stream of String with the processed evaluation
   */
  override def addResult(input: DStream[(Example, Double)]): DStream[String] =
    input.transform(rdd => {
      val inv=rdd.map{case (e,c)=>(c,e)}
      val centr = ClusteringEvaluationUtil.computeAllCentroids(rdd).
                    map{case (k,c,s) => (k,c)}
      inv.join(centr).map{case (k,(e,c))=>pow(e.in.distanceTo(c.in),2)}  
    }).reduce(_+_).map(x=>"SSE=%.5f".format(x))
}

/**
 * Clustering evaluator which computes, for each RDD of Example-Double tuples,
 * the clustering separation measure (SSB). This is the sum of square distances
 * between cluster centroids and the overall data centroid.
 */
class ClusteringSeparationEvaluator extends Evaluator {

  /**
   * Process the result of a predicted stream of Examples and Doubles.
   *
   * @param input the input stream containing (Example,Double) tuples
   * @return a stream of String with the processed evaluation
   */
  override def addResult(input: DStream[(Example, Double)]): DStream[String] =
    input.transform(rdd => {
      val inv=rdd.map{case (e,c) => (c,e)}
      val centr = ClusteringEvaluationUtil.computeAllCentroids(rdd)
      val sumAll=inv.map{case (c,e)=>(e,1)}.reduce((x,y)=>
          (new Example(x._1.in.add(y._1.in)),x._2+y._2))
      val centrAll = {
        if(sumAll._2>1) new Example(sumAll._1.in.map(x=>x/sumAll._2))
        else sumAll._1
      }
      centr.map{case (k,c,s)=>s*pow(c.in.distanceTo(centrAll.in),2)}
    }).reduce(_+_).map(x=>"SSB=%.5f".format(x))
}

/**
 * Helper class which contains util functions for clustering evaluation.
 */
object ClusteringEvaluationUtil {

  def computeAllCentroids(input: RDD[(Example,Double)]): 
                RDD[(Double,Example, Int)] =
    input.map{case (e,c) => (c,e)}.map{case (c,e) => (c,Array(e))}.
      reduceByKey((x,y) => x++y).map{case (c,e) => {
        val clSize = e.length
        val clSum = e.foldLeft(new Example(new NullInstance))(
          (a,x) =>  a.in match {
            case NullInstance() => new Example(x.in.map(x=>x))
            case _ => new Example(a.in.add(x.in))
          })        
        if(clSize>1)
          (c,new Example(clSum.in.map(x=>x/clSize)),clSize)
        else
          (c,clSum,1)
      }}
}
