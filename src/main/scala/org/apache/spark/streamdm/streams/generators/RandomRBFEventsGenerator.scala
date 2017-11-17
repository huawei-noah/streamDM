/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import com.github.javacliparser.{ IntOption, FloatOption, FlagOption }
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.core.specification._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.math._
import org.apache.spark.streamdm.core.specification.ExampleSpecification

/**
 * RandomRBFEventsGenerator generates data stream for Clustream  via a random radial basis function.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Chunk size (<b>-k</b>)
 *  <li> Slide duration (<b>-d</b>)
 *  <li> Seed for random generation of model (<b>-m</b>)
 *  <li> Seed for random generation of instances (<b>-i</b>)
 *  <li> The average number of centroids in the model (<b>-C</b>)
 *  <li> Deviation of the number of centroids in the model (<b>-c</b>)
 *  <li> The average radii of the centroids in the model (<b>-R</b>)
 *  <li> Deviation of average radii of the centroids in the model (<b>-r</b>)
 *  <li> Offset of the average weight a cluster has(<b>-D</b>)
 *  <li> Kernels move a predefined distance of 0.01 every X points (<b>-V</b>)
 *  <li> Speed/Velocity point offset (<b>-v</b>)
 *  <li> Noise level (<b>-N</b>)
 *  <li> Allow noise to be placed within a cluster(<b>-n</b>)
 *  <li> Event frequency (<b>-E</b>)
 *  <li> Enable merging and splitting of clusters(<b>-M</b>)
 *  <li> Enable emerging and disappearing of clusters (<b>-e</b>)
 *  <li> The number of features to generate (<b>-f</b>)
 *  <li> Decay horizon (<b>-h</b>)
 * </ul>
 */

class RandomRBFEventsGenerator extends Generator {

  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 50, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'd',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)

  val modelRandomSeedOption: IntOption = new IntOption("modelRandomSeed",
    'm', "Seed for random generation of model.", 1)

  val instanceRandomSeedOption: IntOption = new IntOption("instanceRandomSeed", 'i',
    "Seed for random generation of instances.", 5)

  val numClusterOption: IntOption = new IntOption("numCluster", 'C',
    "The average number of centroids in the model.", 5, 1, Integer.MAX_VALUE)

  val numClusterRangeOption: IntOption = new IntOption("numClusterRange", 'c',
    "Deviation of the number of centroids in the model.", 3, 0, Integer.MAX_VALUE)

  val kernelRadiiOption: FloatOption = new FloatOption("kernelRadius", 'R',
    "The average radii of the centroids in the model.", 0.07, 0, 1)

  val kernelRadiiRangeOption: FloatOption = new FloatOption("kernelRadiusRange", 'r',
    "Deviation of average radii of the centroids in the model.", 0, 0, 1)

  val densityRangeOption: FloatOption = new FloatOption("densityRange", 'D',
    "Offset of the average weight a cluster has. Value of 0 means all cluster " +
      "contain the same amount of points.", 0, 0, 1)

  val speedOption: IntOption = new IntOption("speed", 'V',
    "Kernels move a predefined distance of 0.01 every X points", 500, 1, Integer.MAX_VALUE)

  val speedRangeOption: IntOption = new IntOption("speedRange", 'v',
    "Speed/Velocity point offset", 0, 0, Integer.MAX_VALUE)

  val noiseLevelOption: FloatOption = new FloatOption("noiseLevel", 'N',
    "Noise level", 0.1, 0, 1)

  val noiseInClusterOption: FlagOption = new FlagOption("noiseInCluster", 'n',
    "Allow noise to be placed within a cluster")

  val eventFrequencyOption: IntOption = new IntOption("eventFrequency", 'E',
    "Event frequency. Enable at least one of the events below and set numClusterRange!", 30000, 0, Integer.MAX_VALUE)

  val eventMergeSplitOption: FlagOption = new FlagOption("eventMergeSplitOption", 'M',
    "Enable merging and splitting of clusters. Set eventFrequency and numClusterRange!")

  val eventDeleteCreateOption: FlagOption = new FlagOption("eventDeleteCreate", 'e',
    "Enable emering and disapperaing of clusters. Set eventFrequency and numClusterRange!")

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "The number of attributes to generate.", 2, 0, Integer.MAX_VALUE)

  val decayHorizonOption: IntOption = new IntOption("decayHorizon", 'h',
    "Decay horizon", 1000, 0, Integer.MAX_VALUE)

  val merge_threshold = 0.7
  val kernelMovePointFrequency = 10
  val maxDistanceMoveThresholdByStep = 0.01
  val maxOverlapFitRuns = 50
  val eventFrequencyRange = 0
  var kernels: ArrayBuffer[GeneratorCluster] = null
  var instanceRandom: Random = null
  var numGeneratedInstances: Int = 0
  var numActiveKernels: Int = 0
  var nextEventCounter: Int = 0
  var nextEventChoice = -1
  var clusterIdCounter: Int = 0
  var mergeClusterA: GeneratorCluster = null
  var mergeClusterB: GeneratorCluster = null
  var mergeKernelsOverlapping = false

  /**
   * A GeneratorCluster represents the cluster which contains
   *  some MicroCluster(represented by SphereCluster) in Clustream
   *  Algorithm.
   */
  class GeneratorCluster {

    var generator: SphereCluster = null
    var kill: Int = -1
    var merging = false
    var moveVector = new Array[Double](numFeaturesOption.getValue())
    var totalMovementSteps: Int = 0
    var currentMovementSteps: Int = 0
    var isSplitting = false
    val microClusters = new ArrayBuffer[SphereCluster]()
    val microClustersDecay = new ArrayBuffer[Int]()

    def this(label: Int) {
      this()
      var outofbounds = true
      var tryCounter = 0
      while (outofbounds && tryCounter < maxOverlapFitRuns) {
        tryCounter = tryCounter + 1
        outofbounds = false
        val center = new Array[Double](numFeaturesOption.getValue())
        var radius = kernelRadiiOption.getValue() + (if (instanceRandom.nextBoolean()) -1 else 1) *
          kernelRadiiRangeOption.getValue() * instanceRandom.nextDouble()
        while (radius <= 0) {
          radius = kernelRadiiOption.getValue() + (if (instanceRandom.nextBoolean()) -1 else 1) *
            kernelRadiiRangeOption.getValue() * instanceRandom.nextDouble()
        }
        for (j <- 0 until numFeaturesOption.getValue() if !outofbounds) {
          center(j) = instanceRandom.nextDouble()
          if (center(j) - radius < 0 || center(j) + radius > 1) {
            outofbounds = true
          }
        }
        generator = new SphereCluster(center, radius)
      }
      if (tryCounter < maxOverlapFitRuns) {
        generator.setId(label)
        val avgWeight = 1.0 / numClusterOption.getValue()
        val weight = avgWeight + (if (instanceRandom.nextBoolean()) -1 else 1) * avgWeight *
          densityRangeOption.getValue() * instanceRandom.nextDouble()
        generator.setWeight(weight)
        setDesitnation(null)
      } else {
        generator = null
        kill = 0
        //println("Tried " + maxOverlapFitRuns + " times to create kernel. Reduce average radii.")
      }
    }

    def this(label: Int, cluster: SphereCluster) {
      this()
      this.generator = cluster
      this.generator.setId(label)
      setDesitnation(null)
    }

    /* *
     *  update the cluster(remove the invalid microcluster. etc.)
     */
    def updateKernel(): Unit = {
      if (kill == 0) {
        var flag = true
        for (i <- 0 until kernels.length if flag) {
          if (kernels(i) == this) {
            kernels.remove(i)
            flag = false
          }
        }
      }
      if (kill > 0) {
        kill = kill - 1
      }
      //we could be lot more precise if we would keep track of timestamps of points
      //then we could remove all old points and rebuild the cluster on up to date point base
      //BUT worse the effort??? so far we just want to avoid overlap with this, so its more
      //konservative as needed. Only needs to change when we need a thighter representation
      for (m <- 0 until microClusters.length) {
        if (numGeneratedInstances - microClustersDecay(m) > decayHorizonOption.getValue()) {
          microClusters.remove(m)
          microClustersDecay.remove(m)
        }
      }
    }

    /* *
   * set the move-path for the cluster 
   *
   * @param destination_t move-destination Array
   */
    def setDesitnation(destination_t: Array[Double]): Unit = {

      var destination: Array[Double] = null
      if (destination_t == null) {
        destination = Array.fill[Double](numFeaturesOption.getValue())(instanceRandom.nextDouble())
      } else {
        destination = destination_t
      }
      val center = generator.getCenter()
      val dim = center.length

      for (d <- 0 until dim) {
        moveVector(d) = destination(d) - center(d)
      }
      var speedInPoints = speedOption.getValue()
      if (speedRangeOption.getValue() > 0)
        speedInPoints = speedInPoints + (if (instanceRandom.nextBoolean()) -1 else 1) *
          instanceRandom.nextInt(speedRangeOption.getValue())
      if (speedInPoints < 1) speedInPoints = speedOption.getValue()

      var length: Double = moveVector.foldLeft(0.0)((sum, i) => sum + pow(i, 2))
      length = Math.sqrt(length)

      totalMovementSteps = (length / (maxDistanceMoveThresholdByStep * kernelMovePointFrequency) * speedInPoints).toInt
      moveVector = moveVector.map { v => v / totalMovementSteps.toDouble }
      currentMovementSteps = 0
    }

    /* *
    * try to merge two cluster.
    * 
    * @param merge the Cluster want to be merged
    * @return info string for merging
    */
    def tryMerging(merge: GeneratorCluster): String = {
      var message = ""
      val overlapDegree: Double = generator.overlapRadiusDegree(merge.generator)
      if (overlapDegree > merge_threshold) {
        val mcluster = merge.generator
        val radius = Math.max(generator.getRadius(), mcluster.getRadius())
        generator.combine(mcluster)
        //adjust radius, get bigger and bigger with high dim data
        generator.setRadius(radius)
        message = "Clusters merging: " + mergeClusterB.generator.getId() + " into " + mergeClusterA.generator.getId()

        //clean up and restet merging stuff
        //mark kernel so it gets killed when it doesn't contain any more instances
        merge.kill = decayHorizonOption.getValue()
        //set weight to 0 so no new instances will be created in the cluster
        mcluster.setWeight(0.0)
        normalizeWeights()
        numActiveKernels = numActiveKernels - 1
        mergeClusterB = null
        mergeClusterA = null
        merging = false
        mergeKernelsOverlapping = false
      } else {
        if (overlapDegree > 0 && !mergeKernelsOverlapping) {
          mergeKernelsOverlapping = true
          message = "Merge overlapping started"
        }
      }
      message
    }

    /* *
    * split a cluster from the cluster
    * 
    * @return message for splitting
    */
    def splitKernel(): String = {
      isSplitting = true
      //todo radius range
      val radius: Double = kernelRadiiOption.getValue()
      val avgWeight: Double = 1.0 / numClusterOption.getValue()
      val weight: Double = avgWeight + avgWeight * densityRangeOption.getValue() * instanceRandom.nextDouble()

      val center = generator.getCenter()
      var spcluster: SphereCluster = new SphereCluster(center, radius, weight)

      if (spcluster != null) {
        val gc: GeneratorCluster = new GeneratorCluster(clusterIdCounter + 1, spcluster)
        clusterIdCounter = clusterIdCounter + 1
        gc.isSplitting = true
        kernels += gc
        normalizeWeights()
        numActiveKernels = numActiveKernels + 1
        "Split from Kernel " + generator.getId()
      } else {
        //System.out.println("Tried to split new kernel from C" + generator.getId() +
        //  ". Not enough room for new cluster, decrease average radii, number of clusters or enable overlap.")
        ""
      }
    }

    /* *
     *  fade out the cluster.
     *  
     * @return message for fading out
     */
    def fadeOut(): String = {
      kill = decayHorizonOption.getValue()
      generator.setWeight(0.0)
      numActiveKernels = numActiveKernels - 1
      normalizeWeights()
      "Fading out C" + generator.getId()
    }

    /* *
     * add an instance.
     * 
     * @param instance the instance want to be added
     */
    def addInstance(instance: Instance): Unit = {
      var minMicroIndex = -1
      var minHullDist = Double.MaxValue
      var inserted = false
      //we favour more recently build clusters so we can remove earlier cluster sooner
      var m = microClusters.length - 1

      while (m >= 0 && !inserted) {
        val micro = microClusters(m)
        val hulldist = micro.getCenterDistance(instance) - micro.getRadius()
        //point fits into existing cluster
        if (hulldist <= 0) {
          //  microClustersPoints.get(m).add(point)
          microClustersDecay.insert(m, numGeneratedInstances)
          inserted = true

        } //if not, check if its at least the closest cluster
        else {
          if (hulldist < minHullDist) {
            minMicroIndex = m
            minHullDist = hulldist
          }
        }
        m = m - 1
      }
      //Reseting index choice for alternative cluster building
      val alt = 1
      if (alt == 1)
        minMicroIndex = -1
      if (!inserted) {
        if (minMicroIndex == -1) {
          var s: SphereCluster = null
          s = new SphereCluster(generator.getCenter(), generator.getRadius(), 1)
          microClusters += s
          microClustersDecay += numGeneratedInstances
          var id = 0
          var exit_flag = false
          while (id < kernels.size && !exit_flag) {
            if (kernels(id) == this)
              exit_flag = true
            id = id + 1
          }
          s.setGroundTruth(id)
        }
      }

    }

    /* *
     * move the cluster.
     */
    def move(): Unit = {
      if (currentMovementSteps < totalMovementSteps) {
        currentMovementSteps = currentMovementSteps + 1
        if (moveVector == null) {
          return
        } else {
          var center = generator.getCenter()
          var outofbounds = true
          while (outofbounds) {
            val radius = generator.getRadius()
            outofbounds = false
            center = generator.getCenter()
            for (d <- 0 until center.length if !outofbounds) {
              center(d) = center(d) + moveVector(d)
              if (center(d) - radius < 0 || center(d) + radius > 1) {
                outofbounds = true
                setDesitnation(null)
              }
            }
          }
          generator.setCenter(center)
        }
      } else {
        if (!merging) {
          setDesitnation(null)
          isSplitting = false
        }
      }
    }

  }

  /* 
   * normalize weights of the cluster.
   */
  def normalizeWeights(): Unit = {

    val sumWeights = kernels.foldLeft(0.0)((sum, k) => sum + k.generator.getWeight())
    kernels.foreach(k => {
      k.generator.setWeight(k.generator.getWeight() / sumWeights)
    })
  }

  /* *
  * init the clusters.
  */
  def initKernels(): Unit = {
    for (i <- 0 until numClusterOption.getValue()) {
      kernels += (new GeneratorCluster(clusterIdCounter))
      numActiveKernels = numActiveKernels + 1
      clusterIdCounter = clusterIdCounter + 1
    }
    normalizeWeights()
  }

  /* *
   * get the code for next event.
   * 
   * @return the code for next event.
   */
  def getNextEvent(): Int = {
    var choice: Int = -1
    var lowerLimit = numActiveKernels <= numClusterOption.getValue() - numClusterRangeOption.getValue()
    var upperLimit = numActiveKernels >= numClusterOption.getValue() + numClusterRangeOption.getValue()

    if (!lowerLimit || !upperLimit) {
      var mode = -1
      if (eventDeleteCreateOption.isSet() && eventMergeSplitOption.isSet()) {
        mode = instanceRandom.nextInt(2)
      }

      if (mode == 0 || (mode == -1 && eventMergeSplitOption.isSet())) {
        //have we reached a limit? if not free choice
        if (!lowerLimit && !upperLimit)
          choice = instanceRandom.nextInt(2)
        else //we have a limit. if lower limit, choose split
        if (lowerLimit)
          choice = 1
        //otherwise we reached upper level, choose merge
        else
          choice = 0
      }

      if (mode == 1 || (mode == -1 && eventDeleteCreateOption.isSet())) {
        //have we reached a limit? if not free choice
        if (!lowerLimit && !upperLimit)
          choice = instanceRandom.nextInt(2) + 2
        else //we have a limit. if lower limit, choose create
        if (lowerLimit)
          choice = 3
        //otherwise we reached upper level, choose delete
        else
          choice = 2
      }
    }
    choice
  }

  /* *
   * get a Weighted-Element.
   * 
   * @return index of the choosen element
   */
  def chooseWeightedElement(): Int = {
    var r = instanceRandom.nextDouble()

    // Determine index of choosen element
    var i = 0 // 0 or 1 kenny?
    while (r > 0.0) {
      r = r - kernels(i).generator.getWeight()
      i = i + 1
    }
    i = i - 1 // Overcounted once
    i
  }

  /* *
   * get a noise.
   * 
   * @return the noise vector
   */
  def getNoisePoint(): Array[Double] = {
    var sample = new Array[Double](numFeaturesOption.getValue())
    var incluster = true
    var counter = 20
    while (incluster) {
      sample = Array.fill(numFeaturesOption.getValue())(instanceRandom.nextDouble())
      incluster = false
      if (!noiseInClusterOption.isSet() && counter > 0) {
        counter = counter - 1
        for (c <- 0 until kernels.length if !incluster) {
          for (m <- 0 until kernels(c).microClusters.length if !incluster) {
            val inst = new DenseInstance(sample)
            if (kernels(c).microClusters(m).getInclusionProbability(inst) > 0) {
              incluster = true
            }
          }
        }
      }
    }
    sample
  }

  /* *
   *  merge clusters.
   * 
   *  @param steps used for choosing clusters to merge
   *  @return info string for merging
   */
  def mergeKernels(steps: Int): String = {

    if (numActiveKernels > 1 && ((mergeClusterA == null && mergeClusterB == null))) {
      //choose clusters to merge
      val diseredDist = steps / speedOption.getValue() * maxDistanceMoveThresholdByStep
      var minDist = Double.MaxValue
      for (i <- 0 until kernels.length) {
        for (j <- 0 until i) {
          if (kernels(i).kill != -1 || kernels(j).kill != -1) {
            //    continue
          } else {
            val kernelDist = kernels(i).generator.getCenterDistance(kernels(j).generator)
            val d = kernelDist - 2 * diseredDist
            if (Math.abs(d) < minDist &&
              (minDist != Double.MaxValue || d > 0 || Math.abs(d) < 0.001)) {
              minDist = Math.abs(d)
              mergeClusterA = kernels(i)
              mergeClusterB = kernels(j)
            }
          }
        }
      }

      if (mergeClusterA != null && mergeClusterB != null) {
        val merge_point = mergeClusterA.generator.getCenter()
        val v = mergeClusterA.generator.getDistanceVector(mergeClusterB.generator)
        for (i <- 0 until v.length) {
          merge_point(i) = merge_point(i) + v(i) * 0.5
        }

        mergeClusterA.merging = true
        mergeClusterB.merging = true
        mergeClusterA.setDesitnation(merge_point)
        mergeClusterB.setDesitnation(merge_point)
        "Init merge"
      } else ""
    } else if (mergeClusterA != null && mergeClusterB != null) {
      //movekernels will move the kernels close to each other,
      //we just need to check and merge here if they are close enough
      mergeClusterA.tryMerging(mergeClusterB)
    } else ""
  }

  /* *
   * split a cluster.
   * 
   * @return message for splitting
   */
  def splitKernel(): String = {

    var id: Int = instanceRandom.nextInt(kernels.size)
    while (kernels(id).kill != -1)
      id = instanceRandom.nextInt(kernels.size)

    val message: String = kernels(id).splitKernel()
    message
  }

  /* *
   * fade out a cluster.
   * 
   * @return message for fading out
   */
  def fadeOut(): String = {
    var id = instanceRandom.nextInt(kernels.size)
    while (kernels(id).kill != -1)
      id = instanceRandom.nextInt(kernels.size)

    val message = kernels(id).fadeOut()
    message
  }

  /* *
   * add a cluster.
   * 
   * @return message for fading in
   */
  def fadeIn(): String = {
    val gc = new GeneratorCluster(clusterIdCounter + 1)
    clusterIdCounter = clusterIdCounter + 1
    kernels += (gc)
    numActiveKernels = numActiveKernels + 1
    normalizeWeights()
    "Creating new cluster"
  }

  /* *
   * eventScheduler
   */
  def eventScheduler(): Unit = {

    kernels.foreach { k => k.updateKernel() }
    nextEventCounter = nextEventCounter - 1
    if (nextEventCounter % kernelMovePointFrequency == 0) {
      kernels.foreach { k => k.move() }
    }
    if (eventFrequencyOption.getValue() == 0) {
      return
    }
    var type_s = ""
    var message = ""
    var eventFinished = false
    nextEventChoice match {
      case 0 =>
        if (numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()) {
          message = mergeKernels(nextEventCounter)
          type_s = "Merge"
        }
        if (mergeClusterA == null && mergeClusterB == null && message.startsWith("Clusters merging")) {
          eventFinished = true
        }
      case 1 =>
        if (nextEventCounter <= 0) {
          if (numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()) {
            type_s = "Split"
            message = splitKernel()
          }
          eventFinished = true
        }
      case 2 =>
        if (nextEventCounter <= 0) {
          if (numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()) {
            message = fadeOut()
            type_s = "Delete"
          }
          eventFinished = true
        }
      case 3 =>
        if (nextEventCounter <= 0) {
          if (numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()) {
            message = fadeIn()
            type_s = "Create"
          }
          eventFinished = true
        }
      case _ => ()

    }
    if (eventFinished) {
      nextEventCounter = (eventFrequencyOption.getValue() + (if (instanceRandom.nextBoolean()) -1 else 1) *
        eventFrequencyOption.getValue() * eventFrequencyRange * instanceRandom.nextDouble()).toInt
      nextEventChoice = getNextEvent()
    }
    if (!message.isEmpty()) {
      message += " (numKernels = " + numActiveKernels + " at " + numGeneratedInstances + ")"
      if (type_s != "Merge" || message.startsWith("Clusters merging")) {

      }
    }
  }

  /* *
   *  initializes the generator
   */
  def init(): Unit = {
    noiseInClusterOption.set()
    instanceRandom = new Random(instanceRandomSeedOption.getValue())
    nextEventCounter = eventFrequencyOption.getValue()
    nextEventChoice = getNextEvent()
    numActiveKernels = 0
    numGeneratedInstances = 0
    clusterIdCounter = 0
    mergeClusterA = null
    mergeClusterB = null
    kernels = new ArrayBuffer[GeneratorCluster]()
    initKernels()
  }

  /* *
   * @return chunk size
   */
  def getChunkSize(): Int = {
    chunkSizeOption.getValue
  }

  /* *
   * @return slide duration
   */
  def getslideDuration(): Int = {
    slideDurationOption.getValue
  }

  /* *
   *  Obtains a single example.
   *  
   *  @return a Example.
   */
  def getExample(): Example = {

    numGeneratedInstances = numGeneratedInstances + 1
    eventScheduler()

    //make room for the classlabel
    val values_new = new Array[Double](numFeaturesOption.getValue())
    var values: Array[Double] = null
    var clusterChoice = -1

    if (instanceRandom.nextDouble() > noiseLevelOption.getValue()) {
      clusterChoice = chooseWeightedElement()
      val ins: Instance = kernels(clusterChoice).generator.sample(instanceRandom)
      values = ins match {
        case d: DenseInstance => d.features
        case s: SparseInstance => null //todo
      }
    } else {
      //get ranodm noise point
      values = getNoisePoint()
    }
    Array.copy(values, 0, values_new, 0, values.length)

    val inputInstance: Instance = new DenseInstance(values_new)

    if (clusterChoice == -1) {
      new Example(inputInstance, new DenseInstance(Array.fill[Double](1)(numClusterOption.getValue().toDouble)))
    } else {
      new Example(inputInstance, new DenseInstance(Array.fill[Double](1)(kernels(clusterChoice).generator.getId())))
    }
  }

  /* *
   *  Obtains the specification of the examples in the stream.
   *  
   *  @return an ExampleSpecification of the features
   */
  override def getExampleSpecification(): ExampleSpecification = {
    //Prepare specification of class attributes
    val outputIS = new InstanceSpecification()
    val classFeature = new NominalFeatureSpecification(Array("false", "true"))
    outputIS.addFeatureSpecification(0, "class", classFeature)
    //Prepare specification of input attributes
    val inputIS = new InstanceSpecification()
    for (i <- 0 until numFeaturesOption.getValue)
      inputIS.addFeatureSpecification(i, "NumericFeature" + i)
    new ExampleSpecification(inputIS, outputIS)
  }
}