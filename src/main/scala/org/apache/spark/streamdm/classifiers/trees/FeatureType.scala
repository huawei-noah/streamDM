package org.apache.spark.streamdm.classifiers.trees

trait FeatureType

case class NullFeatureType() extends FeatureType

case class NominalFeatureType() extends FeatureType

case class NumericFeatureType() extends FeatureType