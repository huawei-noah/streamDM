/**
  * Created by pengsizhe on 2017/12/21.
  */
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.Random
object ONCEStreamingSender {
  var time=0
  def makeSeq() : String=
  {
    var Seq: List[(Int,Int)]=List((-1,-1))
    // val now = new Date()
    // var time=now.getTime//以后使用时间
    for(i<-0 to 50)
    {
      val randomNum=(new Random).nextInt(10)
      time+=1
      if(i==0)
      {
        Seq=List((randomNum,time))
      }
      else
      {
        var newSeq=List((randomNum,time))
        Seq=Seq++newSeq
      }
    }
    return Seq.toString()
  }
  def main(args:Array[String]) =
  {
    val listener =new ServerSocket(8888)
    while(true)
    {
      val socket=listener.accept()
      new Thread()
      {
        override def run(): Unit = {
          println("客户点链接地址"+socket.getInetAddress+":"+socket.getPort)
          var out=new PrintWriter(socket.getOutputStream(),true)
          while(true)
          {
            Thread.sleep(1000)
            var send=makeSeq()+":"
            println(send)
            out.write(send+'\n')
            out.flush()
          }
          socket.close()
        }
      }
        .start()
    }
  }
}

