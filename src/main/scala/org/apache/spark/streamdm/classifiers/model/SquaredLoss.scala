/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.classifiers.model.model

/**
 * Implmenetation of the squared loss function.
 */

class SquaredLoss extends Loss with Serializable {
  /** Computes the value of the loss function
   * @param value the label against which the loss is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the loss value 
   */
  def loss(label: Double, dot: Double): Double =
    0.5*(dot-label)*(dot-label)

  /** Computes the value of the gradient function
   * @param value the label against which the gradient is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the gradient value 
   */
  def gradient(label: Double, dot: Double): Double =
    dot-label

  /** Computes the binary prediction based on a dot prodcut
   * @param dot the dot product of the linear model and the instance
   * @return the predicted binary class
   */
  def predict(dot: Double): Double =
    if (dot>=0) 1 else 0
}
