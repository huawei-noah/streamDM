/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.classifiers.model.model

/**
 * A Loss trait defines the operation needed to compute the loss function, the
 * prediction function, and the gradient for use in a linear Model. 
 */
trait Loss extends Serializable {
  /** Computes the value of the loss function
   * @param value the label against which the loss is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the loss value 
   */
  def loss(label: Double, dot: Double): Double

  /** Computes the value of the gradient function
   * @param value the label against which the gradient is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the gradient value 
   */
  def gradient(label: Double, dot: Double): Double

  /** Computes the binary prediction based on a dot prodcut
   * @param dot the dot product of the linear model and the instance
   * @return the predicted binary class
   */
  def predict(dot: Double): Double
}
