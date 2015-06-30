package org.apache.spark.streamdm.classifiers.trees

import org.scalatest.FunSuite
import org.apache.spark.streamdm.core._

class FeatureClassObserverSuite extends FunSuite {

  test("test NullFeatureClassObserver") {

    val fco: NullFeatureClassObserver = new NullFeatureClassObserver()

  }
}