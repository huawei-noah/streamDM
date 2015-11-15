SPARK_HOME=/home/jianfeng/scala/spark-1.3.0-bin-hadoop2.4
$SPARK_HOME/bin/spark-submit \
  --class "org.apache.spark.streamdm.streamDMJob" \
  --master local[2] \
  /home/jianfeng/git/streamDM/target/scala-2.10/streamdm-spark-streaming-_2.10-0.1.jar \
  $1


