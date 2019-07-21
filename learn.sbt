name := "streamDM"

organization := "huawei-noah"

scalaVersion := "2.11.8"

sparkVersion := "2.3.2"

spName := "huawei-noah/streamDM"

spShortDescription := "streamDM is an open source software for mining big data streams using Spark Streaming"

version := "0.0.1"

licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

spIncludeMaven := false

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")

sparkComponents ++= Seq("streaming")
