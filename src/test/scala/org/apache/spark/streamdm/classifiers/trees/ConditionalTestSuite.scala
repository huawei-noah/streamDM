package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import org.apache.spark.streamdm.core._
class ConditionalTestSuite extends FunSuite {

  test("test NumericBinaryTest") {
    //nb1 not equal test
    val nb1: NumericBinaryTest = new NumericBinaryTest(0, 1, false)
    //nb2 equal test
    val nb2: NumericBinaryTest = new NumericBinaryTest(0, 1, true)
    val example0: Example = new Example(new DenseInstance(Array[Double](0, 5, 5)), null)
    val example1: Example = new Example(new DenseInstance(Array[Double](1, 5, 5)), null)
    val example2: Example = new Example(new DenseInstance(Array[Double](2, 5, 5)), null)
    assert(nb1.branch(example0) == 0)
    assert(nb1.branch(example1) == 1)
    assert(nb1.branch(example2) == 1)
    assert(nb2.branch(example0) == 1)
    assert(nb2.branch(example1) == 0)
    assert(nb2.branch(example2) == 1)
    assert(nb2.maxBranches() == 2)
  }

  test("test NominalBinaryTest") {
    val nb1: NominalBinaryTest = new NominalBinaryTest(0, 1)
    val example0: Example = new Example(new DenseInstance(Array[Double](0, 5, 5)), null)
    val example1: Example = new Example(new DenseInstance(Array[Double](1, 5, 5)), null)
    val example2: Example = new Example(new DenseInstance(Array[Double](2, 5, 5)), null)
    assert(nb1.branch(example0) == 1)
    assert(nb1.branch(example1) == 0)
    assert(nb1.branch(example2) == 1)
    assert(nb1.maxBranches() == 2)
  }

  test("test NominalMultiwayTest") {
    val nb1: NominalMultiwayTest = new NominalMultiwayTest(0, 3)
    val example0: Example = new Example(new DenseInstance(Array[Double](0, 5, 5)), null)
    val example1: Example = new Example(new DenseInstance(Array[Double](1, 5, 5)), null)
    val example2: Example = new Example(new DenseInstance(Array[Double](2, 5, 5)), null)
    assert(nb1.branch(example0) == 0)
    assert(nb1.branch(example1) == 1)
    assert(nb1.branch(example2) == 2)
    assert(nb1.maxBranches() == 3)
  }

  test("test NumericBinaryRulePredicate") {
    //nb1 ==
    val nb1: NumericBinaryRulePredicate = new NumericBinaryRulePredicate(0, 1, 0)
    //nb2 <=
    val nb2: NumericBinaryRulePredicate = new NumericBinaryRulePredicate(0, 1, 1)
    //nb3 <
    val nb3: NumericBinaryRulePredicate = new NumericBinaryRulePredicate(0, 1, 2)
    val example0: Example = new Example(new DenseInstance(Array[Double](0, 5, 5)), null)
    val example1: Example = new Example(new DenseInstance(Array[Double](1, 5, 5)), null)
    val example2: Example = new Example(new DenseInstance(Array[Double](2, 5, 5)), null)
    assert(nb1.branch(example0) == 1)
    assert(nb1.branch(example1) == 0)
    assert(nb1.branch(example2) == 1)
    assert(nb2.branch(example0) == 0)
    assert(nb2.branch(example1) == 0)
    assert(nb2.branch(example2) == 1)
    assert(nb3.branch(example0) == 0)
    assert(nb3.branch(example1) == 1)
    assert(nb3.branch(example2) == 1)
    assert(nb2.maxBranches() == 2)
  }
}