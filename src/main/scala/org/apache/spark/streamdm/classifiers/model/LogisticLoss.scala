/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.classifiers.model.model

import scala.math

/**
 * Implmenetation of the logistic loss function.
 */

class LogisticLoss extends Loss with Serializable {
  /** Computes the value of the loss function
   * @param value the label against which the loss is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the loss value 
   */
  def loss(label: Double, dot: Double): Double = {
    val y = if(label<0) -1 else 1
    math.log(1+math.exp(-y*dot))
  }

  /** Computes the value of the gradient function
   * @param value the label against which the gradient is computed   
   * @param dot the dot product of the linear model and the instance
   * @return the gradient value 
   */
  def gradient(label: Double, dot: Double): Double = {
    val y = if(label<0) -1 else 1
    -y*(1.0-1.0/(1.0+math.exp(-y*dot)))
  }

  /** Computes the binary prediction based on a dot prodcut
   * @param dot the dot product of the linear model and the instance
   * @return the predicted binary class
   */
  def predict(dot: Double): Double = {
    val f = 1.0 / (1.0+math.exp(-dot))
    if (f>0.5) 0 else 1
  }
}
