package org.apache.spark.streamdm.streams.generators

import scala.collection.mutable.HashMap
import scala.collection.mutable.StringBuilder
import java.lang.String
import org.apache.spark.streamdm.core.Instance
import scala.util.Random


abstract class Cluster {

  private var id: Double = -1
  private var gtLabel: Double = -1

  private var measure_values: HashMap[String, String] = _

  def Cluster() {
    measure_values = new HashMap[String, String]()
  }
  /**
   * @return the center of the cluster
   */
  def getCenter(): Array[Double]

  /**
   * Returns the weight of this cluster, not neccessarily normalized.
   * It could, for instance, simply return the number of points contined
   * in this cluster.
   * @return the weight
   */
  def getWeight(): Double

  /**
   * Returns the probability of the given point belonging to
   * this cluster.
   *
   * @param point
   * @return a value between 0 and 1
   */
  def getInclusionProbability(instance: Instance): Double

  def setId(id: Double): Unit = {
    this.id = id
  }

  def getId(): Double = {
    this.id
  }

  def isGroundTruth(): Boolean = {
    gtLabel != -1
  }

  def setGroundTruth(truth: Double): Unit = {
    gtLabel = truth
  }

  def getGroundTruth(): Double = {
    gtLabel
  }

  /**
   * Samples this cluster by returning a point from inside it.
   * @param random a random number source
   * @return an Instance that lies inside this cluster
   */
  def sample(random: Random): Instance

  def setMeasureValue(measureKey: String, value: String) {
    measure_values.put(measureKey, value)
  }

  def setMeasureValue(measureKey: String, value: Double) {
    measure_values.put(measureKey, value.toString)
  }

  def getMeasureValue(measureKey: String): String = {
    measure_values.getOrElse(measureKey, "")
  }
}