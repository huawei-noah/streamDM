SPARK_HOME=/home/albert/Software/spark/spark-1.1.1
$SPARK_HOME/bin/spark-submit \
  --class "StreamingMLJob" \
  --master local[2] \
  ../target/scala-2.10/streaming-logistic-sgd-spark-_2.10-0.1.jar

