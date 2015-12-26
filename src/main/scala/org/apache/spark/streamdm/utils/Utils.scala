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

import java.io._
import java.util.Random
import scala.math.{ min, max, log }

import org.apache.spark.streamdm.classifiers.Classifier

/**
 * Utility methods.
 *
 */
object Utils {

  /* Copy a classifier using serialization
    *
    * @param classifier the original classifier to copy
    * @return the copy of the classifier
    */
  def copyClassifier(classifier: Classifier): Classifier = {
    val baoStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val out: ObjectOutputStream = new ObjectOutputStream(
      new BufferedOutputStream(baoStream))
    out.writeObject(classifier)
    out.flush()
    out.close()
    val byteArray: Array[Byte] = baoStream.toByteArray()
    val in: ObjectInputStream = new ObjectInputStream(new BufferedInputStream(
      new ByteArrayInputStream(byteArray)))
    val copy: Classifier = in.readObject().asInstanceOf[Classifier]
    in.close()
    copy
  }

  /* Compute a random value from a Poisson distribution
   *
   * @param lambda the mean of the Poisson distribution
   * @param r the random generator
   * @return a random value sampled from the distribution
   */
  def poisson(lambda: Double, r: Random) = {
    if (lambda < 100.0) {
      var product = 1.0
      var sum = 1.0
      val threshold = r.nextDouble() * Math.exp(lambda)
      var i = 1.0
      var max = Math.max(100, 10 * Math.ceil(lambda).toInt)
      while ((i < max) && (sum <= threshold)) {
        product *= (lambda / i)
        sum += product
        i += 1.0
      }
      i - 1.0
    } else {
      val x = lambda + Math.sqrt(lambda) * r.nextGaussian()
      if (x < 0.0) 0.0 else Math.floor(x)
    }
  }

  /* Get the most frequent value of an array of numeric values
  *
  * @param array the Array of numeric values
  * @return the argument of the most frequent value
  */
  def majorityVote(array: Array[Double], size: Integer): Double = {
    val frequencyArray: Array[Double] = Array.fill(size)(0)
    for (i <- 0 until array.length)
      frequencyArray(array(i).toInt) += 1
    argmax(frequencyArray)
  }

  /* Get the argument of the minimum value of an array of numeric values
 *
 * @param array the Array of numeric values
 * @return the argument of the minimum value
 */
  def argmax(array: Array[Double]): Double = array.zipWithIndex.maxBy(_._1)._2
  /*
   * Get the log2 of input
   * 
   * @param v double value
   * @return the log2 of v
   */
  def log2(v: Double): Double = log(v) / log(2)

  /* Transpose a matrix
   * 
   * @param input matrix in form of 2-D array
   * @return the transpose of input matrix
   */
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
  /*
   * Split a matrix with the input index, merge other columns into one column and transpose
   * 
   * @param input matrix in form of 2-D array
   * @param fIndex index of columns
   * @return a matrix of 2 rows
   */
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

  /*
   * Normalize input matrix
   * 
   * @param input matrix in form of 2-D array
   * @return normalized matrix
   */
  def normal(input: Array[Array[Double]]): Array[Array[Double]] = {
    val total = input.map(_.sum).sum
    input.map { row => row.map { _ / total } }
  }

  /*
   * Normalize input array
   * 
   * @param input double array
   * @return normalized array
   */
  def normal(input: Array[Double]): Array[Double] = {
    val total = input.sum
    input.map { { _ / total } }
  }

}
