package org.apache.spark.streamdm.util

object Util {

  def argmax(array: Array[Double]): Double = array.zipWithIndex.maxBy(_._1)._2

  def transpose(input: Array[Array[Double]]): Array[Array[Double]] = {
    val output: Array[Array[Double]] = Array.fill(input(0).length)(new Array[Double](input.length))
    input.zipWithIndex.map {
      row =>
        row._1.zipWithIndex.map {
          col => output(col._2)(row._2) = input(row._2)(col._2)
        }
    }
    output
  }
  def splitTranspose(input: Array[Array[Double]], fIndex: Int): Array[Array[Double]] = {
    val output: Array[Array[Double]] = Array.fill(2)(new Array[Double](input.length))
    input.zipWithIndex.map {
      row =>
        row._1.zipWithIndex.map {
          col =>
            if (col._2 == fIndex) output(0)(row._2) = input(row._2)(col._2)
            else output(1)(row._2) += input(row._2)(col._2)
        }
    }
    output
  }
  def main(args:Array[String])={
        val input: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6))
    val output = Util.splitTranspose(input, 1)
    println(output.length == 2)
    println(output(0).length == 2)
    println(output(0)(0) == 2)
    println(output(0)(1) == 5)
    println(output(1)(0) == 4)
    println(output(1)(1) == 10)
  }
}