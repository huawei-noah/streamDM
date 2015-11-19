/**
 *
 */
package org.apache.spark.streamdm.streams.generators

/**
 * Stream generator for a random radial basis function stream with drift
 *
 * this file refers to the RandomRBFGeneratorDrift.java in MOA.
 *
 * <p>It uses the following options:
 * <ul>
 *  <li> Speed of change of centroids in the model (<b>-s</b>)
 *  <li> the number of centroids with drift (<b>-r</b>)
 * </ul>
 */
import org.apache.spark.streamdm.streams.StreamReader
import org.apache.spark.streamdm.streams.StreamReader
import com.github.javacliparser.{ IntOption, FloatOption, StringOption }
import org.apache.spark.streaming.dstream.{ DStream, InputDStream }
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.streams.StreamReader
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.collection._

class RandomRBFDriftGenerator extends RandomRBFGenerator {

/* override val chunkSizeOption: IntOption = new IntOption("chunkSize", 'k',
    "Chunk Size", 10000, 1, Integer.MAX_VALUE)

  override val slideDurationOption: IntOption = new IntOption("slideDuration", 'w',
    "Slide Duration in milliseconds", 1000, 1, Integer.MAX_VALUE)
 
   override val modelRandomSeedOption: IntOption = new IntOption("modelRandomSeed",'d',
      "Seed for random generation of model.", 1, 1, Integer.MAX_VALUE)

  override val instanceRandomSeedOption: IntOption = new IntOption("instanceRandomSeed", 'i',
      "Seed for random generation of instances.", 1, 1, Integer.MAX_VALUE)

  override val numClassesOption: IntOption = new IntOption("numClasses", 'c',
    "The number of classes to generate.", 2, 2, Integer.MAX_VALUE)

  override val numFeaturesOption: IntOption = new IntOption("numFeatures", 'a',
    "The number of features to generate.", 4, 0, Integer.MAX_VALUE)

  override val numCentroidsOption: IntOption = new IntOption("numCentroids", 'n',
    "The number of centroids in the model.", 50, 1, Integer.MAX_VALUE)

  override val instanceTypeOption: StringOption = new StringOption("instanceType", 't',
    "Type of the instance to use", "dense")*/
   
  val speedChangeOption: FloatOption = new FloatOption("speedChange", 's',
    "Speed of change of centroids in the model.", 2.0, 0, Float.MaxValue);

  val numDriftCentroidsOption: IntOption = new IntOption("numDriftCentroids", 'r',
    "The number of centroids with drift.", 50, 0, Integer.MAX_VALUE);

  private var speedCentroids: Array[Array[Double]] = null

 /* override val centroids = new Array[Centroid](numCentroidsOption.getValue)

  override  val centroidWeights = new Array[Double](centroids.length)

  override val instanceRandom: Random = new Random(instanceRandomSeedOption.getValue())
  */
  override def init(): Unit = {
    if(!inited) {
      this.generateCentroids
      inited = true
    }
  }
  
  /**
   * Obtains a stream of examples.
   *
   * @param ssc a Spark Streaming context
   * @return a stream of Examples
   */
   
  override def getExample(): Example = {
    //Update Centroids with drift
    var len: Int = 0
    if (numDriftCentroidsOption.getValue() > centroids.length) {
      len = centroids.length
    } else len = numDriftCentroidsOption.getValue()

    for (j <- 0 until len) {
      for (i <- 0 until numFeaturesOption.getValue()) {
        centroids(j).centre(i) += speedCentroids(j)(i) * speedChangeOption.getValue();
        if (centroids(j).centre(i) > 1) {
          centroids(j).centre(i) = 1
          speedCentroids(j)(i) = -speedCentroids(j)(i)
        } else if (centroids(j).centre(i) < 0) {
          centroids(j).centre(i) = 0
          speedCentroids(j)(i) = -speedCentroids(j)(i)
        }
      }
    }
    super.getExample()
  }

  /**
   *
   * generate centroids based on the drift parameters
   */
  override def generateCentroids() {
    super.generateCentroids() 
    val modelRand: Random = new Random(modelRandomSeedOption.getValue())

    var len: Int = 0
    if (numDriftCentroidsOption.getValue() > centroids.length) {
      len = centroids.length
    } else len = numDriftCentroidsOption.getValue()

    speedCentroids = Array.ofDim[Double](len, this.numFeaturesOption.getValue())
    for (i <- 0 until len) {
      val randSpeed = new Array[Double](numFeaturesOption.getValue())
      var normSpeed: Double = 0.0
      for (j <- 0 until randSpeed.length) {
        randSpeed(j) = modelRand.nextDouble()
        normSpeed += randSpeed(j) * randSpeed(j)
      }
      normSpeed = Math.sqrt(normSpeed)
      for (j <- 0 until randSpeed.length) {
        randSpeed(j) /= normSpeed
      }
      speedCentroids(i) = randSpeed
    }
  }

  /**
   * Obtains the specification of the examples in the stream.
   *
   * @return an ExampleSpecification of the features
   */

  override def getExampleSpecification(): ExampleSpecification = {
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
