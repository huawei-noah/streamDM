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
import math._

/**
 * Test suite for the DenseInstance.
 */
class DenseInstanceSuite extends FunSuite {

  test("return its features given indices") {
    val instance = DenseInstance(Array(1.4,1.3,2.1))
    assert(instance(0) == 1.4)
    assert(instance(1) == 1.3)
    assert(instance(2) == 2.1)
    }

  test("return 0.0 for a out of bounds index") {
    val instance = DenseInstance(Array(1.4,1.3,2.1))
    assert(instance(-1) == 0.0)
    assert(instance(3) == 0.0)
    }

  test("have a dot operation with another DenseInstance") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = DenseInstance(Array(0.4, 0.3, 1.1))
    assert(instance1.dot(instance2) == 1.4*0.4+1.3*0.3+2.1*1.1)
  }

  test("add another DenseInstance") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = DenseInstance(Array(0.4, 0.3, 1.1))
    val sumInstance = instance1.add(instance2);
    val instance3 = DenseInstance(Array(1.4+0.4, 1.3+0.3, 2.1+1.1))
    (sumInstance.features zip instance3.features).map{case (x,y)=> assert(x==y)}
  }

  test("add a SparseInstance") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = SparseInstance(Array(1), Array(1.2))
    val sumInstance = instance1.add(instance2);
    val instance3 = DenseInstance(Array(1.4+0, 1.3+1.2, 2.1+0))
    (sumInstance.features zip instance3.features).map{case (x,y)=> assert(x==y)}
  }

  test("compute the distance to another DenseInstance") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = DenseInstance(Array(0.4, 0.3, 1.1))
    val dist = instance1.distanceTo(instance2);
    val trueDist = math.sqrt(math.pow(1.4-0.4,2)+math.pow(1.3-0.3,2)+
                   math.pow(2.1-1.1,2))
    assert(dist==trueDist)
  }

  test("compute the distance to a SparseInstance") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = SparseInstance(Array(1), Array(1.2))
    val dist = instance1.distanceTo(instance2);
    val trueDist = math.sqrt(math.pow(1.4,2)+math.pow(1.3-1.2,2)+
                   math.pow(2.1,2))
    assert(dist==trueDist)
  }

  test("set the value of a Feature") {
    val instance1 = DenseInstance(Array(1.1, 1.3, 2.1))
    val instance2 = instance1.set(1,2.0)
    val instance3 = DenseInstance(Array(1.1, 2.0, 2.1))
    (instance2.features zip instance3.features).map{case (x,y) => assert(x==y)}
  }

  test("append a Feature") {
    val instance1 = DenseInstance(Array(1.1, 1.3, 2.1))
    val instance2 = instance1.set(3,2.0)
    val instance3 = DenseInstance(Array(1.1, 1.3, 2.1, 2.0))
    (instance2.features zip instance3.features).map{case (x,y) => assert(x==y)}
  }

  test("function for features") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    val instance2 = instance1.map(f=>f+2.0)
    (instance1.features zip instance2.features).map{case (x,y)=> 
      assert(y==x+2.0)}
  }

  test("parse from CSV format") {
    val input = "1.1,1.3,2.1"
    val parsedInstance = DenseInstance.parse(input)
    val testInstance = DenseInstance(Array(1.1,1.3,2.1))
    (parsedInstance.features zip testInstance.features).map{ case (x,y) =>
      assert(x==y)}
  }

  test("have a .toString override") {
    val instance1 = DenseInstance(Array(1.4, 1.3, 2.1))
    assert(instance1.toString == "1.4,1.3,2.1")
  }
}
