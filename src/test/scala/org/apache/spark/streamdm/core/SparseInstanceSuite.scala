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
 * Test suite for the SparseInstance.
 */
class SparseInstanceSuite extends FunSuite {

  test("return its features given indices") {
    val instance = SparseInstance(Array(0,1,2), Array(1.4,1.3,2.1))
    assert(instance(0) == 1.4)
    assert(instance(1) == 1.3)
    assert(instance(2) == 2.1)
    }

  test("return 0.0 for a out of bounds index") {
    val instance = SparseInstance(Array(0,2), Array(1.4,2.1))
    assert(instance(-1) == 0.0)
    assert(instance(1) == 0.0)
    assert(instance(3) == 0.0)
    }

  test("have a dot operation with another SparseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4,2.1))
    val instance2 = SparseInstance(Array(1,2),Array(1.2,3.2))
    assert(instance1.dot(instance2) == 2.1*3.2)
  }

  test("have a dot operation with a DenseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4,2.1))
    val instance2 = DenseInstance(Array(1.2,2.1,3.2))
    assert(instance1.dot(instance2) == 1.4*1.2+2.1*3.2)
  }

  test("able to add another SparseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4, 2.1))
    val instance2 = SparseInstance(Array(1,2), Array(1.6, 2.0))
    val sumInstance = instance1.add(instance2);
    val instance3 = SparseInstance(Array(0,1,2), Array(1.4, 1.6, 2.1+2.0))
    sumInstance.indexes.map{case x => assert(sumInstance(x)==instance3(x))}
  }

  test("able to add a DenseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4, 2.1))
    val instance2 = DenseInstance(Array(0.0, 1.6, 2.0))
    val sumInstance = instance1.add(instance2);
    val instance3 = SparseInstance(Array(0,1,2), Array(1.4, 1.6, 2.1+2.0))
    sumInstance.indexes.map{ case x => assert(sumInstance(x)==instance3(x))}
  }

  test("able to compute the distance to another SparseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4, 2.1))
    val instance2 = SparseInstance(Array(1,2), Array(1.6, 2.0))
    val dist = instance1.distanceTo(instance2);
    val trueDist = math.sqrt(math.pow(1.4,2)+math.pow(1.6,2)+
                   math.pow(2.1-2.0,2))
    assert(dist==trueDist)
  }

  test("able to compute the distance to a DenseInstance") {
    val instance1 = SparseInstance(Array(0,2), Array(1.4, 2.1))
    val instance2 = DenseInstance(Array(0.0, 1.6, 2.0))
    val dist = instance1.distanceTo(instance2);
    val trueDist = math.sqrt(math.pow(1.4,2)+math.pow(1.6,2)+
                   math.pow(2.1-2.0,2))
    assert(dist==trueDist)
  }

  test("able to append a Feature") {
    val instance1 = SparseInstance(Array(1), Array(1.1))
    val instance2 = instance1.set(2,2.1)
    val instance3 = SparseInstance(Array(1,2), Array(1.1, 2.1))
    instance2.indexes.map{ case x => assert(instance2(x)==instance3(x))}
  }

  test("have a map function for features") {
    val instance1 = SparseInstance(Array(1,2), Array(1.4, 1.3))
    val instance2 = instance1.map(f=>f+2.0)
    instance2.indexes.map{ case x => assert(instance2(x)==instance1(x)+2.0)}
  }

  test("have a parser in comma separated LibSVM format")
  {
    val input = "1:1.1,3:2.1"
    val parsedInstance = SparseInstance.parse(input)
    val testInstance = SparseInstance(Array(0,2),Array(1.1,2.1))
    parsedInstance.indexes.map{ case x =>
      assert(parsedInstance(x)==testInstance(x))}
  }

  test("have a .toString override") {
    val instance1 = SparseInstance(Array(1,2), Array(1.4, 1.3))
    assert(instance1.toString == "2:%f,3:%f".format(1.4,1.3))
  }
}
