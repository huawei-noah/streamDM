name := "streamDM (Spark Streaming)"

version := "0.2"

scalaVersion := "2.11.8"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.3.2"

libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.3.2"

libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka" % "1.6.3"

libraryDependencies += "org.scalatest"  %% "scalatest"   % "3.0.8" % Test
