/**
 * TODO license goes here
 */

package org.apache.spark.streamdm.regression

import org.apache.spark.streamdm._
import org.apache.spark.streamdm.input._
import org.apache.spark.streamdm.model._

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream._

/**
 * The SGDLearner trains a LinearModel using the stochastic gradient descent
 * algorithm. At construction, the type of loss function, the lambda learning
 * reate parameter, and the number of features need to be specified
 */
class SGDLearner(lossFunction: Loss, lambdaParam: Double,
                 numFeatures: Int) 
  extends Learner with Serializable {
  
  type T = DenseSingleLabelInstance
    
  val lambda = lambdaParam
  var model = new LinearModel[T](lossFunction,
                              new DenseSingleLabelInstance(
                                Array.fill[Double](numFeatures+1)(0.0),0.0))
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
                        reduce((x,y) => x.add(y)).
                        map(x => x.mapFeatures(x=>lambda*x))
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
  override def predict(input: DStream[T]): DStream[(T,Double)] = 
    input.map(x => (x, model.predict(x)))
}
