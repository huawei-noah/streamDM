/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.classifiers.trees

import org.apache.spark.streaming.dstream._
import org.apache.spark.Logging

import org.apache.spark.streamdm.core._
import org.apache.spark.streamdm.classifiers.Classifier

class IncrementalClassfier extends Classifier {

  type T = IncrementalModel

  var model: IncrementalModel = null

  var exampleLearnerSpecification: ExampleSpecification = null

  /* Init the model based on the algorithm implemented in the learner,
   * from the stream of instances given for training.
   *
   */
  override def init(exampleSpecification: ExampleSpecification): Unit = {
    exampleLearnerSpecification = exampleSpecification
    model = new IncrementalModel()
  }

  /* Train the model based on the algorithm implemented in the learner, 
   * from the stream of instances given for training.
   *
   * @param input a stream of instances
   * @return the updated Model
   */
  override def train(input: DStream[Example]): Unit = {
    input.foreachRDD {
      rdd =>
        val tmodel = rdd.aggregate(
          new IncrementalModel(model))(
            (mod, example) => { mod.update(example) }, (mod1, mod2) => mod1.merge(mod2))
        model = new IncrementalModel(tmodel)
    }
  }

  /* Predict the label of the Instance, given the current Model
   *
   * @param instance the Instance which needs a class predicted
   * @return a tuple containing the original instance and the predicted value
   */
  override def predict(input: DStream[Example]): DStream[(Example, Double)] = {
    input.map { x => (x, model.predict(x)) }
  }

  /* Gets the current Model used for the Learner.
   * 
   * @return the Model object used for training
   */
  override def getModel: IncrementalModel = model

}

class IncrementalModel(val base: Int = 0) extends Model with Serializable with Logging {
  type T = IncrementalModel

  var flag: Boolean = true

  var count = 0

  var baseObserver: Observer = new Observer()

  var blockOberver: Observer = new Observer()
  def this(that: IncrementalModel) = {
    this(that.base + that.count)
    this.baseObserver = that.baseObserver.merge(that.blockOberver)
  }
  override def update(change: Example): IncrementalModel = {
    if (flag) {
      logInfo(this + "\nbase:" + base)
      logInfo("baseObserver" + baseObserver.count)
      flag = false
    }
    count += 1
    blockOberver.observer(1)
    this
  }

  def predict(instance: Example): Double = {
    0.0
  }

  def merge(that: IncrementalModel): IncrementalModel = {
    logInfo(this + "\n" + that)
    logInfo("before merge:" + this.base + "," + this.count + ":" + that.base + "," + that.count)
    //    logInfo("after merge:" +this.count+"+"+that.count+"="+(this.count+that.count))
    this.baseObserver = this.baseObserver.merge(that.blockOberver)
    logInfo("baseObserver:" + this.baseObserver.count)
    this
  }

}

class Observer(var count: Int = 0) extends Serializable {
  def observer(weight: Int): Unit = {
    count += weight
  }
  def merge(that: Observer): Observer = {
    count += that.count
    this
  }

}