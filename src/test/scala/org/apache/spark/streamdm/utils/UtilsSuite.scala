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
package org.apache.spark.streamdm.utils

import org.scalatest.FunSuite

/**
 * Test suite for Utils
 */
class UtilsSuite extends FunSuite {

  test("The Majority Vote should return the most frequent item") {
    assert(Utils.majorityVote(Array(1, 2, 1, 0), 3) == 1)
    assert(Utils.majorityVote(Array(1, 2, 2, 0), 3) == 2)
    assert(Utils.majorityVote(Array(1, 2, 0, 0), 3) == 0)
    assert(Utils.majorityVote(Array(1, 0, 1, 2), 3) == 1)

  }

  test("test transpose") {
    val input: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6))
    val output = Utils.transpose(input)
    assert(output.length == 3)
    assert(output(0).length == 2)
    assert(output(0)(0) == input(0)(0))
    assert(output(0)(1) == input(1)(0))
    assert(output(1)(0) == input(0)(1))
    assert(output(1)(1) == input(1)(1))
    assert(output(2)(0) == input(0)(2))
    assert(output(2)(1) == input(1)(2))
  }

  test("test splitTranspose") {
    val input: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6))
    val output = Utils.splitTranspose(input, 1)
    assert(output.length == 2)
    assert(output(0).length == 2)
    assert(output(0)(0) == 2)
    assert(output(0)(1) == 5)
    assert(output(1)(0) == 4)
    assert(output(1)(1) == 10)
  }

}
