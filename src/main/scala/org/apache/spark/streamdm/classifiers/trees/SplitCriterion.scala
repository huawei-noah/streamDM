package org.apache.spark.streamdm.classifier.trees

import scala.math.{ log, max }

trait SplitCriterion extends Serializable {
  def getMerit(pre: Array[Double], post: Array[Array[Double]]): Double
  def getRangeMerit(pre: Array[Double]): Double
}

class InfoGainSplitCriterion(val minBranch: Double = 0.01) extends SplitCriterion with Serializable {

  override def getMerit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    if (numFrac(post, minBranch) < 2) Double.NegativeInfinity
    else entropy(pre) - entropy(post)
  }

  override def getRangeMerit(pre: Array[Double]): Double = log(max(pre.length, 2))

  def entropy(pre: Array[Double]): Double = {
    if (pre == null || pre.sum <= 0) 0
    log(pre.sum) - pre.filter(_ > 0).map(x => x * log(x)).sum / pre.sum
  }

  def entropy(post: Array[Array[Double]]): Double = {
    if (post == null || post.length == 0 || post(0).length == 0) 0
    val tpost: Array[Array[Double]] = Array.fill(post(0).length)(new Array[Double](post.length))
    post.zipWithIndex.map {
      x => { x._1.zipWithIndex.map { y => tpost(x._2)(y._2) = post(y._2)(x._2) } }
    }
    tpost.map { x => entropy(x) * x.sum }.sum / post.map(_.sum).sum
  }

  private[trees] def numFrac(post: Array[Array[Double]], minFrac: Double): Int = {
    if (post == null || post.length == 0) 0
    val sums: Array[Double] = Array[Double](post(0).length)
    post.map { x => (x.zipWithIndex.map { ix => sums(ix._2) += ix._1 }) }
    sums.filter(_ > sums.sum * minFrac).length
  }
}