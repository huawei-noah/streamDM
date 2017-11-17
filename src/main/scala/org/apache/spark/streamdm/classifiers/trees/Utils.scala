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
package org.apache.spark.streamdm.classifiers.trees

import scala.math.min

object Utils {
  /*
   * Add two arrays and return the min length of two input arrays.
   * 
   * @param array1 double Array
   * @param array2 double Array
   * @return Array which added in each index
   */
  def addArrays(array1: Array[Double], array2: Array[Double]): Array[Double] = {
    val merge = new Array[Double](min(array1.length, array2.length))
    for (i <- 0 until merge.length)
      merge(i) = array1(i) + array2(i)
    merge
  }

  /*
   * Return a string for a matrix
   * 
   * @param m  matrix in form of 2-D array
   * @param split string of the split
   * @param head string of matrix head and each line's head
   * @param tail string of matrix tail and each line's tail
   * @return string format of input matrix
   */
  def matrixtoString[T](m: Array[Array[T]], split: String = ",", head: String = "{", tail: String = "}"): String = {
    val sb = new StringBuffer(head)
    for (i <- 0 until m.length) {
      sb.append(head)
      for (j <- 0 until m(i).length) {
        sb.append(m(i)(j))
        if (j < m(i).length - 1)
          sb.append(split)
      }
      sb.append(tail)
      if (i < m.length - 1)
        sb.append(split)
    }
    sb.append(tail).toString()
  }

  /*
   * Return a string for an array
   * 
   * @param m  array
   * @param split string of the split
   * @param head string of array head
   * @param tail string of array tail
   * @return string format of input array
   */
  def arraytoString[T](pre: Array[T], split: String = ",", head: String = "{", tail: String = "}"): String = {
    val sb = new StringBuffer(head)
    for (i <- 0 until pre.length) {
      sb.append(pre(i))
      if (i < pre.length - 1)
        sb.append(split)
    }
    sb.append(tail).toString()
  }
}