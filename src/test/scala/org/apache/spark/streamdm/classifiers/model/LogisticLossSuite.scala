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

package org.apache.spark.streamdm.classifiers.model

import org.scalatest.FunSuite

/**
 * Test suite for the Logistic Loss.
 */
class LogisticLossSuite extends FunSuite {

  test("The Logistic Loss function must compute the value of the loss function") {
    val loss = new LogisticLoss
    assert (loss.loss(1, 0) == math.log(2))
    assert (loss.loss(1, 1) == math.log(1+math.exp(-1)))
  }

  test("It must compute the gradient of the loss function") {
    val loss = new LogisticLoss
    assert (loss.gradient(0, 0) == .5)
    assert (loss.gradient(1, 0) == -.5)
  }

  test("It must compute the prediction using the loss function") {
    val loss = new LogisticLoss
    assert (loss.predict(1) == 1)
    assert (loss.predict(0) == 0)

  }

}
