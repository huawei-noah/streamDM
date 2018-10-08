#!/usr/bin/env bash
$SPARK_HOME/bin/spark-submit \
  --class "org.apache.spark.streamdm.streamDMJob" \
  --master local[2] \
  ../target/scala-2.11/streamdm-spark-streaming-_2.11-0.2.jar \
  $1
