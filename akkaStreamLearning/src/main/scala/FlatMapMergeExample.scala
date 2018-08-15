import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._

object FlatMapMergeExample {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("QuickStart2")
    //使具体化,使物质化,使具体
    //materializer 他是一个数据流的执行工厂;使数据流的run()可以执行;你只需知道调用run()来使他执行
    implicit val materializer = ActorMaterializer()


    Source(1 to 6)
      .flatMapMerge(5, i ⇒ Source(List.fill(1)(i*i)))
      .runWith(Sink.foreach(println))


  }





}
