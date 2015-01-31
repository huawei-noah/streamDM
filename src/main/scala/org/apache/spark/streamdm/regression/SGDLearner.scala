/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.regression

import com.github.javacliparser.{ClassOption, FloatOption, IntOption}
import org.apache.spark.streamdm._
import org.apache.spark.streamdm.input._
import org.apache.spark.streamdm.model._
import org.apache.spark.streaming.dstream._

/**
 * The SGDLearner trains a LinearModel using the stochastic gradient descent
 * algorithm. At construction, the type of loss function, the lambda learning
 * reate parameter, and the number of features need to be specified
 */
class SGDLearner()
  extends Learner {

  val numFeaturesOption: IntOption = new IntOption("numFeatures", 'f',
    "Number of Features", 3, 1, Integer.MAX_VALUE)

  val lambdaOption: FloatOption = new FloatOption("lambda", 'l',
    "Lambda", .1, 0, Float.MaxValue)

  val lossFunctionOption:ClassOption = new ClassOption("lossFunction", 'o',
    "Loss function to use", classOf[Loss], "LogisticLoss")

  type T = DenseSingleLabelInstance

  var model: LinearModel[T] = null

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(): Unit = {
    model = new LinearModel[T](lossFunctionOption.getValue(),
      new DenseSingleLabelInstance(
        Array.fill[Double](numFeaturesOption.getValue + 1)(0.0), 0.0))

  }

  /* Train the model using stochastic gradient descent.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[T]): Unit = {
    //train the changes
    //first, compute the gradient
    //then, add the gradients together
    //finally, apply lambda
    val changes = input.map(x => model.gradient(x)).
      reduce((x, y) => x.add(y)).
      map(x => x.mapFeatures(x => lambdaOption.getValue * x))
    //apply the final changes to the new model
    changes.foreachRDD(rdd => {
      model = model.update(rdd.first())
    })
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[T]): DStream[(T, Double)] =
    input.map(x => (x, model.predict(x)))
}
