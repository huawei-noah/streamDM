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
 * Test suite for the Example.
 */
class ExampleSuite extends FunSuite {

  test("return its features given indices") {
    val example =  new Example(SparseInstance(Array(1,2), Array(1.4, 1.3)),
      DenseInstance(Array(1.0)))
    assert(example.featureAt(0)==0.0)
    assert(example.featureAt(1)==1.4)
    assert(example.featureAt(2)==1.3)
  }

  test("return its labels given indices") {
    val example =  new Example(SparseInstance(Array(1,2), Array(1.4, 1.3)),
      DenseInstance(Array(1.0)))
    assert(example.labelAt(0)==1.0)
    assert(example.labelAt(1)==0.0)
  }

  test("able to parse an input-only format")
  {
    val input = "1:1.1,3:2.1"
    val parsedExample = Example.parse(input,"sparse","dense")
    val testExample = new Example(SparseInstance(Array(0,2),Array(1.1,2.1)))
    assert(parsedExample.featureAt(0)==testExample.featureAt(0))
    assert(parsedExample.featureAt(1)==testExample.featureAt(1))
    assert(parsedExample.featureAt(2)==testExample.featureAt(2))
    assert(parsedExample.labelAt(0)==0)
    assert(parsedExample.weight==1.0)
  }

  test("able to parse an input-output format")
  {
    val input = "1 1:1.1,3:2.1"
    val parsedExample = Example.parse(input,"sparse","dense")
    val testExample = new Example(SparseInstance(Array(0,2),Array(1.1,2.1)),
      DenseInstance(Array(1.0)))
    assert(parsedExample.featureAt(0)==testExample.featureAt(0))
    assert(parsedExample.featureAt(1)==testExample.featureAt(1))
    assert(parsedExample.featureAt(2)==testExample.featureAt(2))
    assert(parsedExample.labelAt(0)==testExample.labelAt(0))
    assert(parsedExample.weight==1.0)
  }

  test("able to parse an input-output-weight format")
  {
    val input = "1 1:1.1,3:2.1 3.2"
    val parsedExample = Example.parse(input,"sparse","dense")
    val testExample = new Example(SparseInstance(Array(0,2),Array(1.1,2.1)),
      DenseInstance(Array(1.0)))
    assert(parsedExample.featureAt(0)==testExample.featureAt(0))
    assert(parsedExample.featureAt(1)==testExample.featureAt(1))
    assert(parsedExample.featureAt(2)==testExample.featureAt(2))
    assert(parsedExample.labelAt(0)==testExample.labelAt(0))
    assert(parsedExample.weight==3.2)
  }

  test("should have a .toString override for input-only Examples") {
    val instance1 =  new Example(SparseInstance(Array(1,2), Array(1.4, 1.3)))
    assert(instance1.toString == "2:%f,3:%f".format(1.4,1.3))
  }

  test("should have a .toString override for input-output Examples") {
    val instance1 =  new Example(SparseInstance(Array(1,2), Array(1.4, 1.3)),
      DenseInstance(Array(1.0)))
    assert(instance1.toString == "1.0 2:%f,3:%f".format(1.4,1.3))
  }

  test("should have a .toString override for input-output-weight Examples") {
    val instance1 =  new Example(SparseInstance(Array(1,2), Array(1.4, 1.3)),
      DenseInstance(Array(1.0)),2.1)
    assert(instance1.toString == "1.0 2:%f,3:%f %f".format(1.4,1.3,2.1))
  }

}
