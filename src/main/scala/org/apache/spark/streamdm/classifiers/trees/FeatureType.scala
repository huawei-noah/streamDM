package org.apache.spark.streamdm.classifiers.trees

trait FeatureType {
  def getRange(): Int = 0
}

case class NullFeatureType() extends FeatureType with Serializable

case class NominalFeatureType(val range: Int) extends FeatureType with Serializable {
  override def getRange(): Int = range
}

case class NumericFeatureType() extends FeatureType with Serializable

class FeatureTypeArray(val featureTypes: Array[FeatureType]) extends Serializable {

}