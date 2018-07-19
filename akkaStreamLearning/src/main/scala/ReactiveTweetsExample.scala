
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, impl}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future


/**
  * Author: fei2
  * Date:  18-7-13 下午4:34
  * Description:
  * Refer To:
  */
object ReactiveTweetsExample {


  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") ⇒ Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  val akkaTag = Hashtag("#akka")

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("reactive-tweets")
    //ActorMaterializer 这个参数是可以配置的
    /**
      * Source[Out,M1] Flow[In,Out,M2] Sink[In,M3];其中 M1 M2 M3 和数据处理没有关系;只和materialized types 物化类型有关系
      *
      */
    implicit val materializer = ActorMaterializer()

    val tweets: Source[Tweet,NotUsed] = Source(
        Tweet(Author("1111"),System.currentTimeMillis(),"#111")::
        Tweet(Author("rolandkuhn"),System.currentTimeMillis(),"#akka rocks!")::
        Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
        Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
        Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
        Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
        Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
        Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
        Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
        Tweet(Author("2222"), System.currentTimeMillis, "222!") ::
        Nil
    )
    //数据流的简单处理;和Scala Colletion 相似;但是这个是操作在一个动态得到数据流上的
    val authors: Source[Author, NotUsed] =
      tweets
        .filter(_.hashtags.contains(akkaTag))
        .map(_.author)
//    authors.runWith(Sink.foreach(println))

    //使流中的数据扁平化
    //flatmap 先映射,在拍扁;小集合变成一个大的集合
    /**
      * 1. map会将每一条输入映射为一个新对象。{苹果，梨子}.map(去皮） = {去皮苹果，去皮梨子}
      * 其中：   “去皮”函数的类型为：A => B
      * 2.flatMap包含两个操作：会将每一个输入对象输入映射为一个新集合，然后把这些新集合连成一个大集合。
      * {苹果，梨子}.flatMap(切碎)  = {苹果碎片1，苹果碎片2，梨子碎片1，梨子碎片2}
      * 其中：“切碎”函数的类型为： A => List<B>
      */
    val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)
//    hashtags.runWith(Sink.foreach(println))


    //broadcast  广播;一个流分支成多个流
    val writeAuthors: Sink[Author, Future[Done]] = Sink.ignore
    val writeHashtags: Sink[Hashtag, Future[Done]] = Sink.ignore

    // format: OFF
    //#graph-dsl-broadcast
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val bcast = b.add(Broadcast[Tweet](2))
      tweets ~> bcast.in
      bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
      bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
      ClosedShape
    })
    g.run()


    //背压策略
    def slowComputation(t: Tweet): Long = {
      Thread.sleep(500) // act as if performing some heavy computation
      42
    }
    tweets
      .buffer(10,OverflowStrategy.dropHead)
      .map(slowComputation)
      .runWith(Sink.ignore)

    //物化计算数值 materialize
    import scala.concurrent._
    import scala.concurrent.ExecutionContext.Implicits.global


    val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ ⇒ 1)

    val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

    val counterGraph: RunnableGraph[Future[Int]] =
      tweets
        .via(count)
        .toMat(sumSink)(Keep.right)

    val sum: Future[Int] = counterGraph.run()

    sum.foreach(c ⇒ println(s"Total tweets processed: $c"))
    //物化计算的简单版
    val sumSimple: Future[Int] = tweets.map(t ⇒ 1).runWith(sumSink)
    println(sumSimple.value)
  }

}
