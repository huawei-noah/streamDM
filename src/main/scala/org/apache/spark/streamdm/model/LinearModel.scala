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
class LinearModel[I<:Instance[I]](lossFunction: Loss, initialModel: I)
  extends Model[LinearModel[I],I] with Serializable {
  
  val loss = lossFunction
  val modelInstance = initialModel
  /* Update the model, depending on an Instance given for training
   *
   * @param instance the Instance based on which the Model is updated
   * @return the updated Model
   */
  override def update(changeInstance: I): LinearModel[I] =
    new LinearModel[I](loss, modelInstance.add(changeInstance)) 

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a Double representing the class predicted
   */
  def predict(instance: I): Double =
    loss.predict(modelInstance.dot(instance.append(1.0)))

  /* Compute the loss of the direction of the change
   * @param instance the Instance for which the gradient is computed
   * @return an instance containging the gradients for every feature
   */
  def gradient(instance: I): I = {
    //compute the gradient based on the dot product, then compute the changes
    val ch = loss.gradient(instance.label,
                           modelInstance.dot(instance.append(1.0)))
    instance.mapFeatures(x => ch*x)
  }
}
