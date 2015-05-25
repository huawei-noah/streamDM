package org.apache.spark.streamdm.util

import org.scalatest.FunSuite
class UtilSuite extends FunSuite {
  test("test transpose") {
    val input: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6))
    val output = Util.transpose(input)
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
    val output = Util.splitTranspose(input, 1)
    assert(output.length == 2)
    assert(output(0).length == 2)
    assert(output(0)(0) == 2)
    assert(output(0)(1) == 5)
    assert(output(1)(0) == 4)
    assert(output(1)(1) == 10)
  }
}