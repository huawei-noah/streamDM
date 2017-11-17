name := "streamDM (Spark Streaming)"

version := "0.2"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.0"

libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "2.2.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.3"
