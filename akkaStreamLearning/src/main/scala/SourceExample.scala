import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future

/**
  * Author: fei2
  * Date:  18-8-14 上午10:52
  * Description: akka stream 流的操作
  * Refer To:https://www.cnblogs.com/tiger-xc/p/7364500.html
  */
object SourceExample {

  implicit val system = ActorSystem("demo")
  implicit val materializer: Materializer = ActorMaterializer.create(system)

  implicit val ec = system.dispatcher

  val s1: Source[Int, NotUsed] = Source(1 to 10)
  val sink: Sink[Any, Future[Done]] = Sink.foreach(println)

  def main(args: Array[String]): Unit = {
//    SourceExample()
    flowExample()
  }

  def SourceExample(): Unit ={

    val rg1: RunnableGraph[NotUsed] = s1.to(sink)

    val rg2: RunnableGraph[Future[Done]] = s1.toMat(sink)(Keep.right)

    val res1: NotUsed = rg1.run()

    Thread.sleep(1000)

    val res2: Future[Done] = rg2.run()

    res2.andThen {
      case _ => system.terminate()
    }
  }

  def flowExample(): Unit ={

    val seq = Seq[Int](1,2,3)
    def toIterator() = seq.iterator
    val flow1: Flow[Int,Int,NotUsed] = Flow[Int].map(_ + 2)
    val flow2: Flow[Int,Int,NotUsed] = Flow[Int].map(_ * 3)

    val s2 = Source.fromIterator(toIterator)
    //非顺序执行;println结构语句划分不准确
    println("-----------s2---------------")
//    s2.runForeach(println)   //1 2 3
    println("-----------s3---------------")
    //起到连接作用 s1 的尾 连接 到 s2 的头
    //从上面例子里的组合结果类型我们发现：把一个Flow连接到一个Source上形成了一个新的Source。
    val s3 = s1 ++ s2   //1 2 3 4 5 6 7 8 9 10 1 2 3
//    s3.toMat(sink)(Keep.right).run()

    println("-----------s4---------------")
    val s4: Source[Int,NotUsed] = s3.viaMat(flow1)(Keep.right)
//    s4.toMat(sink)(Keep.right).run() //3 4 5 6 7 8 9 10 11 12 3 4 5

    println("-----------s5---------------")
    //aync的作用是指定左边的graph在一个独立的actor上运行
    val s5: Source[Int,NotUsed] = s3.via(flow1).async.viaMat(flow2)(Keep.right)
//    s5.toMat(sink)(Keep.right).run() //9 12 15 18 21 24 27 30 33 36 9 12 15

    println("-----------s6---------------")
    val s6: Source[Int,NotUsed] = s4.async.viaMat(flow2)(Keep.right)


    val fres = s1.runFold(0)(_ + _)
    fres.onSuccess{case a => println(a)}
    fres.andThen{case _ => system.terminate()}

/*    s6.toMat(sink)(Keep.right).run().andThen({  //9 12 15 18 21 24 27 30 33 36 9 12 15
      case _ => system.terminate()
    })*/
  }


}