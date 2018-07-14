import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
/**
  * akka stream quickstart
  *
  * akka actor 系统 acotr 发送消息;接受消息;处理消息;
  * akka stream 和 actor的映射 数据流转化为消息流;actor转化为处理数据流的函数
  * 我们能不能够让角色变成一堆函数的调用，来对我们想要实现的功能进行抽象? 我们可否把角色的消息作为函数的输入和输出, 并且是类型安全的? 欢迎你, Akka-Streams.
  *
  * Akka Streams API 和 reactive stream api 操作不一样
  * akka stream 底层流 就是 reactive stream
  * stream 提供了 source flow sink 三个组件 还有高级的Graph
  */
object QuickExample extends App {



  implicit val system = ActorSystem("QuickStart")
  //使具体化,使物质化,使具体
  //materializer 他是一个数据流的执行工厂;使数据流的run()可以执行;你只需知道调用run()来使他执行
  implicit val materializer = ActorMaterializer()

  //第一个参数为数据源类型,第二个参数没有使用就可以好用NotUse;
  // (e.g. a network source may provide information about the bound port or the peer’s address)

  val source :Source[Int,NotUsed]= Source(1 to 100)

  //pass this little stream setup to an Actor that runs it.
  //在此示例中，我们将数字打印到控制台 - 并将此小流设置传递给运行它的Actor。
  // 通过将“run”作为方法名称的一部分来发信号让ACTOR 进行处理;
  // 还有其他运行Akka Streams的方法，它们都遵循这种模式。
  source.runForeach(i => println(i))(materializer)
  //1-100 每一个数的阶乘

  val factorials = source.scan(BigInt(1))((acc,next) => acc * next)
  val result = factorials
    .map(num => ByteString(s"$num\n"))
    .runWith(FileIO.toPath(Paths.get("factorials.txt")))
  //Reusable Pieces
  //可重用的部分为Sink ,为一个数据状态
  //这个方法需要返回一个 类型为Sink[String,Future[IOResult]],的sink;其中String 为输入参数类型;Future[IOResult]为输出参数类型
  //物化参数从 source -> flow ,从左 到右 保留结果为 FileIO.toPath 中的Future[IOResult],为右边,所以keep.right.

  //keep right参数是让这个SInk的第二个泛型参数(Future[IOResult])跟toMat(FileIO.toPath(Paths.get(filename))中的一致（keep left则是与调用toMat的对象保持一致，这个在最后run返回的对象是有影响的
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  factorials.map(_.toString()).runWith(lineSink("factorial2.txt"))

  factorials
    .zipWith(Source(0 to 100))((num, idx) ⇒ s"$idx! = $num")
    //放慢处理速度一秒一次
    .throttle(1, 1.second)
    .runForeach(println)
  Thread.sleep(110000)
  System.exit(0)

}
