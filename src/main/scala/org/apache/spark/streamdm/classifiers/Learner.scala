/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.classifiers

import com.github.javacliparser.Configurable
import org.apache.spark.streamdm.core._
import org.apache.spark.streaming.dstream._

/**
 * A Learner trait defines the needed operations on any learner implemented. It
 * provides methods for training the model and for predicting the labels for a
 * stream of Instance RDDs.
 */
trait Learner extends Configurable  with Serializable {

  type T <: Instance[T]

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  def init(): Unit

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  def train(input: DStream[T]): Unit

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  def predict(input: DStream[T]): DStream[(T,Double)]
}
