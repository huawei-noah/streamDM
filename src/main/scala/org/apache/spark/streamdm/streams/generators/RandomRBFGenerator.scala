/**
 *
 */
package org.apache.spark.streamdm.streams.generators

import com.github.javacliparser.{ IntOption, FloatOption, StringOption }
import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.io._
import java.io._

class RandomRBFGenerator extends Generator {
  
  val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 10000, 1, Integer.MAX_VALUE)

  val slideDurationOption: IntOption = new IntOption("slideDuration", 'w',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)
 
  val modelRandomSeedOption: IntOption = new IntOption("modelRandomSeed",
    'r', "Seed for random generation of model.", 1)

  val instanceRandomSeedOption: IntOption = new IntOption("instanceRandomSeed",
    'i', "Seed for random generation of instances.", 1)

  val numClassesOption: IntOption = new IntOption("numClasses", 'c',
    "The number of classes to generate.", 2, 2, Integer.MAX_VALUE)

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'a',
    "The number of features to generate.", 4, 0, Integer.MAX_VALUE)

  val numCentroidsOption: IntOption = new IntOption("numCentroids", 'n',
    "The number of centroids in the model.", 50, 1, Integer.MAX_VALUE)

  val instanceOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")

  class Centroid(center: Array[Double], classLab: Int, stdev: Double) extends Serializable {
    val centre = center
    val classLabel = classLab
    val stdDev = stdev
  }

  val centroids = new Array[Centroid](numCentroidsOption.getValue)

  val centroidWeights = new Array[Double](centroids.length)

  val instanceRandom: Random = new Random(instanceRandomSeedOption.getValue())
  
  override def init(): Unit = {
    generateCentroids
  }
  
  override def getChunkSize(): Int = {
    chunkSizeOption.getValue
  }
  
  def getslideDuration(): Int = {
    slideDurationOption.getValue
  }
  
  /**
   * Get a single example.
   * @return an example
   */
  def getExample(): Example = {
    val index = chooseRandomIndexBasedOnWeights(centroidWeights, instanceRandom)
    val centroid: Centroid = centroids(index)
    val numFeatures = numFeaturesOption.getValue()
    /*
    val featureVals = new Array[Double](numFeatures)
    for (i <- 0 until numFeatures) {
      featureVals(i) = instanceRandom.nextDouble() * 2.0 - 1.0
    }
    var magnitude: Double = 0.0
    for (i <- 0 until numFeatures) {
      magnitude += (featureVals(i) * featureVals(i))
    }
    magnitude = Math.sqrt(magnitude)
    */
    val initFeatureVals:Array[Double] = Array.fill[Double](numFeatures)(instanceRandom.nextDouble()*2.0-1.0)
    val magnitude = Math.sqrt(initFeatureVals.foldLeft(0.0){(a,x) => a + x*x})
    
    println("magnitude " + magnitude)

    val desiredMag = instanceRandom.nextGaussian() * centroid.stdDev
    val scale = desiredMag / magnitude
    
    /*for (i <- 0 until numFeatures) {
      featureVals(i) = centroid.centre(i) + featureVals(i) * scale
    }
    */
    val featureVals = centroid.centre zip initFeatureVals map {case (a,b) => a + b*scale}

    val inputInstance: Instance = new DenseInstance(featureVals)
    //new Example(inputInstance, new DenseInstance(Array(centroid.classLabel)), centroidWeights(index))
    val example = new Example(inputInstance, new DenseInstance(Array(centroid.classLabel)))
    example
  }

  def chooseRandomIndexBasedOnWeights(weights: Array[Double], random: Random): Int = {
    val probSum = weights.reduceLeft[Double](_ + _)
    val ran = random.nextDouble() * probSum;
    var index: Int = 0
    var sum: Double = 0.0
    while ((sum <= ran) && (index < weights.length)) {
      sum += weights(index)
      index += 1
    }
    index - 1
  }

  def generateCentroids(): Unit = {
    val modelRand: Random = new Random(modelRandomSeedOption.getValue());
    /*
    for (i <- 0 until centroids.length) {
      val randCentre = new Array[Double](numFeaturesOption.getValue());
      for (j <- 0 until randCentre.length) {
        randCentre(j) = modelRand.nextDouble();
      }

      centroids(i) = new Centroid(randCentre, modelRand.nextInt(numClassesOption.getValue()),
        modelRand.nextDouble());
      centroidWeights(i) = modelRand.nextDouble();
    }
    */
    for (i <- 0 until centroids.length) {
      val randCentre: Array[Double] = Array.fill[Double](numFeaturesOption.getValue)(modelRand.nextDouble())
      centroids.update(i, new Centroid(randCentre, modelRand.nextInt(numClassesOption.getValue), modelRand.nextDouble()))
      centroidWeights.update(i, modelRand.nextDouble())
    }
  }

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */
  def getExampleSpecification(): ExampleSpecification = {

    //Prepare specification of class attributes
    val outputIS = new InstanceSpecification()
    val classFeature = new NominalFeatureSpecification(Array("+", "-"))
    outputIS.setFeatureSpecification(0, classFeature)
    outputIS.setName(0, "class")

    //Prepare specification of input attributes
    val inputIS = new InstanceSpecification()
    for (i <- 1 to numFeaturesOption.getValue) inputIS.setName(i, "Feature" + i)

    new ExampleSpecification(inputIS, outputIS)

  }
}