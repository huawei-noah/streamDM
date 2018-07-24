package org.apache.spark.streamdm.clusterers.clusters
import org.apache.spark.streamdm.core.Example
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap

/*
 *  Cache class keeps a hashmap where it 
 *  stores the number of the bucket that holds
 *  the major coreset and the coreset itself
 *  r is the merge threshold in this case
 *  we use r = 2 since we implemented an 2-way 
 *  coreset tree 
 */


class Cache(val r: Int) extends Serializable {

  val cacheMap: HashMap[Int,Array[Example]] = new HashMap[Int,Array[Example]]()
  var counter: Int = 0  // the number of the buckets

  def size: Int = cacheMap.size

  def incrementsCounter(): Unit = {
    counter += 1
  }

  def getCounter: Int = counter
  /*
   * insert the major coreset in the hashmap 
   * filter the coresets that exists in the partsum list
   */
  def insertCoreset(n: Int,coreset:Array[Example] ): Unit = {
    cacheMap.put(n,coreset)
    val partsumList = partSum(n)
    val filteredCache = cacheMap.filter{ case (k, _) => partsumList.contains(k) }
  }


  def removeCoreset(n: Int): Unit = {
    cacheMap.remove(n)
  }

  def getCoreset(n: Int): Array[Example] = cacheMap.getOrElse(n, null)

  /**
    * return the partial sums of number n
    *
    * @param n
    * @return
    */
  private def partSum(n: Int) = {
    val queue = new Queue[Int]()
    val sumQueue = new Queue[Int]()
    var weight = 1
    var num = n
    while (num > 0) {
      val a = num % r
      num = num / r
      if (a != 0) queue.enqueue(a * weight)
      weight = weight * r
    }
    var temp = 0
    for(i<- queue.size - 1  to 1 by -1 ) {
       temp += queue(i)
       sumQueue.enqueue(temp)
     }
     sumQueue
  }

  def major(n: Int): Int = n - minor(n)

  def minor(n: Int): Int = {
    var num = n
    if (num == 0) return 0
    var weight = 1
    while (num%r == 0){
      num = num / r
      weight = weight * r
    }
    val minor = weight * (num % r)
    minor
  }

  /**
    * Start from level 0
    *
    * @param n
    * @return
    */
  def minorLevel(n: Int): Int = {
    var level = 0
    var num = n
    while (num % r ==0) {
      num = num / r
      level += 1
    }
    level
  }
}
