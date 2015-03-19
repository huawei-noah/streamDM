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

package org.apache.spark.streamdm.core

import org.scalatest.FunSuite

/**
 * Test suite for the DenseInstance.
 */
class DenseInstanceSuite extends FunSuite {

  test("A Dense Instance should return its features given indices") {
    val instance = DenseSingleLabelInstance(Array(1.4,1.3,2.1), 1.0)
    assert(instance.featureAt(0) == 1.4)
    assert(instance.featureAt(1) == 1.3)
    assert(instance.featureAt(2) == 2.1)
    }

  test("It should return its class label") {
    val instance = DenseSingleLabelInstance(Array(1.4,1.3,2.1), 1.0)
    assert(instance.labelAt(0) == 1.0)
  }

  test("It should have a dot operation") {
    val instance1 = DenseSingleLabelInstance(Array(1.4, 1.3, 2.1), 1.0)
    val instance2 = DenseSingleLabelInstance(Array(1.4, 1.3, 2.1), 1.0)
    assert(instance1.dot(instance2) == 8.06)
  }

  test("It should have an add operation of instances") {
    val instance1 = DenseSingleLabelInstance(Array(1.4, 1.3, 2.1), 1.0)
    val instance2 = DenseSingleLabelInstance(Array(1.4, 1.3, 2.1), 1.0)
    val sumInstance = instance1.add(instance2);
    val instance3 = DenseSingleLabelInstance(Array(1.4+1.4, 1.3+1.3, 2.1+2.1), 1.0)
    (sumInstance.features zip instance3.features).map{case (x,y)=> assert(x==y)}
    assert(instance1.add(instance2).labelAt(0) == instance3.labelAt(0))
  }

  test("It should have a map function for features") {
    val instance1 = DenseSingleLabelInstance(Array(1.4, 1.3, 2.1), 1.0)
    val instance2 = instance1.mapFeatures(f=>f+2.0)
    (instance1.features zip instance2.features).map{case (x,y)=> assert(y==x+2.0)}
    assert(instance2.labelAt(0) == instance1.labelAt(0))
  }
}