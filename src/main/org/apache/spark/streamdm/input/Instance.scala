/*
 * TODO license goes here
 */

package org.apache.spark.streamdm.input

/**
 * An Instance represents the input of any learning algorithm. It is normally
 * composed of a feature vector (with various implementations) and an optional
 * label vector
 */

trait Instance[T] extends Serializable { self:T =>
  val label: Double
  /** Get the feature value present at position index
   *
   * @param index the index of the features
   * @return a Double representing the feature value
   */
  def featureAt(index: Int): Double
  
  /** Get the class value present at position index
   *
   * @param index the index of the class
   * @return a Double representing the value fo the class
   */
  def labelAt(index: Int): Double

  /** Perform a dot product between two instances
   *
   * @param input an Instance with which the dot product is performed
   * @return a Double representing the dot product 
   */
  def dot(input: T): Double

  /** Perform an element by element addition between two instances
   *
   * @param input an Instance which is added up
   * @return an Instance representing the added Instances
   */
  def add(input: T): T

  /** Append a feature to the instance
   *
   * @param input the value which is added up
   * @return an Instance representing the new feature vector
   */
  def append(input: Double): T

  /** Apply an operation to every feature of the Instance (essentially a map)
   *
   * @param func the function for the transformation
   * @return a new Instance with the transformed features
   */
  def mapFeatures(func: Double=>Double): T
}
