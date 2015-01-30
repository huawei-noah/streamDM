/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.model

import org.apache.spark.streamdm.input._

/**
 * A Model trait defines the needed operations on any learning Model. It
 * provides methods for updating the model and for predicting the label of a
 * given Instance
 */
trait Model[T,I<:Instance[I]] extends Serializable { self:T =>
  /* Update the model, depending on the Instance given for training
   *
   * @param changeInstance the Instance based on which the Model is updated
   * @return the updated Model
   */
  def update(changeInstance : I): T

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  def predict(instance : I): Double
}
