package org.apache.spark.streamdm.classifiers.trees

import scala.math.{ log, max }

trait SplitCriterionType
case class InfoGainSplitCriterionType() extends SplitCriterionType
case class GiniSplitCriterionType() extends SplitCriterionType
case class VarianceReductionSplitCriterionType() extends SplitCriterionType

trait SplitCriterion extends Serializable {
  def merit(pre: Array[Double], post: Array[Array[Double]]): Double
  def rangeMerit(pre: Array[Double]): Double
  def negtive(pre: Array[Double]): Boolean = (pre.filter(x => x < 0).length > 0)
}

class InfoGainSplitCriterion(val minBranch: Double = 0.01) extends SplitCriterion with Serializable {

  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = {
    if (numFrac(post, minBranch) < 2) Double.NegativeInfinity
    else entropy(pre) - entropy(post)
  }

  override def rangeMerit(pre: Array[Double]): Double = log(max(pre.length, 2))

  def entropy(pre: Array[Double]): Double = {
    if (pre == null || pre.sum <= 0 || negtive(pre)) 0.0
    log(pre.sum) - pre.filter(_ > 0).map(x => x * log(x)).sum / pre.sum
  }

  def entropy(post: Array[Array[Double]]): Double = {
    if (post == null || post.length == 0 || post(0).length == 0) 0
    val tpost: Array[Array[Double]] = Array.fill(post(0).length)(new Array[Double](post.length))
    post.zipWithIndex.map {
      x => { x._1.zipWithIndex.map { y => tpost(y._2)(x._2) = post(x._2)(y._2) } }
    }
    tpost.map { x => entropy(x) * x.sum }.sum / post.map(_.sum).sum
  }

  private[trees] def numFrac(post: Array[Array[Double]], minFrac: Double): Int = {
    if (post == null || post.length == 0) 0
    val sums: Array[Double] = new Array[Double](post(0).length)
    post.map { _.zipWithIndex.map { x => sums(x._2) += x._1 } }
    sums.filter(_ > sums.sum * minFrac).length
  }
}

class GiniSplitCriterion extends SplitCriterion with Serializable {
  //todo
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = 0
  override def rangeMerit(pre: Array[Double]): Double = 0
}
class VarianceReductionSplitCriterion extends SplitCriterion with Serializable {
  //todo
  override def merit(pre: Array[Double], post: Array[Array[Double]]): Double = 0
  override def rangeMerit(pre: Array[Double]): Double = 0
}

object SplitCriterion {
  def createSplitCriterion(scType: SplitCriterionType, minBranch: Double = 0.01): SplitCriterion = scType match {
    case infoGrain: InfoGainSplitCriterionType   => new InfoGainSplitCriterion(minBranch)
    case gini: GiniSplitCriterionType            => new GiniSplitCriterion()
    case vr: VarianceReductionSplitCriterionType => new VarianceReductionSplitCriterion()
    case _                                       => new InfoGainSplitCriterion(minBranch)
  }
}