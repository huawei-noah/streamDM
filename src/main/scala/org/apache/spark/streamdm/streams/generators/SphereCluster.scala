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

package org.apache.spark.streamdm.streams.generators
import scala.util.Random
import org.apache.spark.streamdm.core._

/**
 * A class for defining a kind of MicroCluster
 */
class SphereCluster extends Cluster {

  private var center: Array[Double] = _
  private var radius: Double = 0.0
  private var weight: Double = 0.0

  def this(center: Array[Double], radius: Double, weightedSize: Double) {
    this()
    this.center = center
    this.radius = radius
    this.weight = weightedSize
  }

  def this(center: Array[Double], radius: Double) {
    this(center, radius, 1.0)
  }

  def this(dimensions: Int, radius: Double, random: Random) {
    this()
    this.radius = radius

    // Position randomly but keep hypersphere inside the boundaries
    val interval: Double = 1.0 - 2 * radius
    this.center = Array.fill[Double](dimensions)(random.nextDouble()*interval + radius)
    this.weight = 0.0
  }

  /**
   * Checks whether two <code>SphereCluster</code> overlap based on radius
   * NOTE: overlapRadiusDegree only calculates the overlap based
   * on the centers and the radi, so not the real overlap
   */

  def overlapRadiusDegree(other: SphereCluster): Double = {
    val center0: Array[Double] = getCenter()
    val radius0: Double = getRadius()

    val center1: Array[Double] = other.getCenter()
    val radius1: Double = other.getRadius()

    var radiusBig: Double = 0.0
    var radiusSmall: Double = 0.0
    if (radius0 < radius1) {
      radiusBig = radius1
      radiusSmall = radius0
    } else {
      radiusBig = radius0
      radiusSmall = radius1
    }

    val dist:Double = Math.sqrt((center0 zip center1 map {case(a,b)=>a-b}).foldLeft(0.0)((a,x)=>a+x*x))
    if (dist > radiusSmall + radiusBig)
      0
    else if (dist + radiusSmall <= radiusBig) {
      //one lies within the other
      1
    } else {
      (radiusSmall + radiusBig - dist) / (2 * radiusSmall)
    }
  }

  def combine(cluster: SphereCluster): Unit = {
    var center: Array[Double] = getCenter()
    val newcenter: Array[Double] = new Array[Double](center.length)
    val other_center: Array[Double] = cluster.getCenter()
    val other_weight: Double = cluster.getWeight()
    val other_radius: Double = cluster.getRadius()

    for (i <- 0 until center.length)
      newcenter(i) = (center(i) * getWeight() + other_center(i) * other_weight) / (getWeight() + other_weight)

    center = newcenter
    val r_0: Double = getRadius() + Math.abs(distance(center, newcenter))
    val r_1: Double = other_radius + Math.abs(distance(other_center, newcenter))
    radius = Math.max(r_0, r_1)
    weight += other_weight
  }

  def merge(cluster: SphereCluster): Unit = {
    val c0 = getCenter()
    val w0 = getWeight()
    val r0 = getRadius()

    var c1 = cluster.getCenter()
    var w1 = cluster.getWeight()
    var r1 = cluster.getRadius()

    val v:Array[Double] = c0 zip c1 map{case (a,b)=>a-b}
    val d:Double = Math.sqrt(v.foldLeft(0.0){(a,x)=> a + x*x})
    
    var r: Double = 0
    var c: Array[Double] = new Array[Double](c0.length)

    //one lays within the others
    if (d + r0 <= r1 || d + r1 <= r0) {
      if (d + r0 <= r1) {
        r = r1
        c = c1
      } else {
        r = r0
        c = c0
      }
    } else {
      r = (r0 + r1 + d) / 2.0
      for (i <- 0 until c.length) {
        c(i) = c1(i) - v(i) / d * (r1 - r)
      }
    }

    setCenter(c)
    setRadius(r)
    setWeight(w0 + w1)
  }

  @Override
  def getCenter(): Array[Double] = {
    val copy: Array[Double] = new Array[Double](center.length)
    Array.copy(center, 0, copy, 0, center.length)
    copy
  }

  def setCenter(center: Array[Double]): Unit = {
    this.center = center
  }

  def getRadius(): Double = {
    radius
  }

  def setRadius(radius: Double): Unit = {
    this.radius = radius
  }

  @Override
  def getWeight(): Double = {
    weight
  }

  def setWeight(weight: Double): Unit = {
    this.weight = weight
  }

  @Override
  def getInclusionProbability(instance: Instance): Double = {
    if (getCenterDistance(instance) <= getRadius()) {
      1.0
    } else
      0.0
  }

  def getCenterDistance(instance: Instance): Double = {
    var distance: Double = 0.0
    //get the center through getCenter so subclass have a chance
    val center: Array[Double] = getCenter()
    for (i <- 0 until center.length) {
      val d: Double = center(i) - instance(i)
      distance += d * d
    }
    Math.sqrt(distance)
  }

  def getCenterDistance(other: SphereCluster): Double = {
    distance(getCenter(), other.getCenter())
  }

  /*
   * the minimal distance between the surface of two clusters.
   * is negative if the two clusters overlap
   * 
   * @param sphereCluster
   * @return the minimal distance
   */
  def getHullDistance(other: SphereCluster): Double = {
    var dist: Double = 0
    //get the center through getCenter so subclass have a chance
    val center0: Array[Double] = getCenter()
    val center1: Array[Double] = other.getCenter()
    dist = distance(center0, center1)
    dist = distance(center0, center1) - (this.getRadius() + other.getRadius())
    dist
  }


  /**
   * When a clusters looses points the new minimal bounding sphere can be
   * partly outside of the originating cluster. If a another cluster is
   * right next to the original cluster (without overlapping), the new
   * cluster can be overlapping with this second cluster. OverlapSave
   * will tell you if the current cluster can degenerate so much that it
   * overlaps with cluster 'other'
   *
   * @param other the potentially overlapping cluster
   * @return true if cluster can potentially overlap
   */
  def overlapSave(other: SphereCluster): Boolean = {
    //use basic geometry to figure out the maximal degenerated cluster
    //comes down to Max(radius *(sin alpha + cos alpha)) which is
    val minDist: Double = Math.sqrt(2) * (getRadius() + other.getRadius())
    val diff: Double = getCenterDistance(other) - minDist
    if (diff > 0)
      true
    else
      false
  }

  def distance(v1: Array[Double], v2: Array[Double]): Double = {
    val distance:Double = Math.sqrt((v1 zip v2 map{case (a,b)=>a-b}).foldLeft(0.0)((a,x)=>a+x*x))
    distance
  }

/*  def getDistanceVector(instance: Instance): Array[Double] = {
    val vec: Array[Double] = instance match {
      case d: DenseInstance  => d.features
      case s: SparseInstance => null //todo
    }
    if (vec != null) {
      distanceVector(getCenter(), instance match {
        case d: DenseInstance  => d.features
        case s: SparseInstance => null //todo
      })
    } else null
  }*/

  def getDistanceVector(other: SphereCluster): Array[Double] = {
    distanceVector(getCenter(), other.getCenter())
  }

  def distanceVector(v1: Array[Double], v2: Array[Double]): Array[Double] = {
    v1 zip v2 map{case (a,b)=> a-b}   
  }

  /**
   * Samples this cluster by returning a point from inside it.
   * @param random a random number source
   * @return a point that lies inside this cluster
   */
  def sample(random: Random): Instance = {
    // Create sample in hypersphere coordinates
    //get the center through getCenter so subclass have a chance
    val center: Array[Double] = getCenter()

    val dimensions: Int = center.length

    var sin: Array[Double] = new Array[Double](dimensions - 1)
    var cos: Array[Double] = new Array[Double](dimensions - 1)
    val length = random.nextDouble() * getRadius()

    var lastValue: Double = 1.0
    for (i <- 0 until dimensions - 1) {
      val angle: Double = random.nextDouble() * 2 * Math.PI
      sin(i) = lastValue * Math.sin(angle) // Store cumulative values
      cos(i) = Math.cos(angle)
      lastValue = sin(i)
    }

    // Calculate cartesian coordinates
    var res: Array[Double] = new Array[Double](dimensions)

    // First value uses only cosines
    res(0) = center(0) + length * cos(0)

    // Loop through 'middle' coordinates which use cosines and sines
    for (i <- 1 until dimensions - 1) {
      res(i) = center(i) + length * sin(i - 1) * cos(i)
    }

    // Last value uses only sines
    res(dimensions - 1) = center(dimensions - 1) + length * sin(dimensions - 2)

    new DenseInstance(res)
  }
}