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
 * Test suite for the Hinge Loss.
 */
class HingeLossSuite extends FunSuite {

  test("The Hinge Loss function must compute the value of the loss function") {
    val loss = new HingeLoss
    assert (loss.loss(1, 0) == 1)
    assert (loss.loss(1, 1) == 0)

  }

  test("It must compute the gradient of the loss function") {
    val loss = new HingeLoss
    assert (loss.gradient(0, .5) == 1)
    assert (loss.gradient(1, .5) == -1)
  }

  test("It must compute the prediction using the loss function") {
    val loss = new HingeLoss
    assert (loss.predict(1) == 1)
    assert (loss.predict(-1) == 0)

  }

}
