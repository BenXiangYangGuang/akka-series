import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}

/**
  * Author: fei2
  * Date:  18-8-14 下午3:49
  * Description:graph 图构建示例
  * Refer To:https://www.cnblogs.com/tiger-xc/p/7403931.html
  */
object SimpleGraphs {

  def main(args: Array[String]): Unit = {
    implicit val sys = ActorSystem("streamSys")
    implicit val ec = sys.dispatcher
    implicit val mat = ActorMaterializer()

    val source = Source(1 to 10)
    val flow = Flow[Int].map(_ * 2)
    val sink = Sink.foreach(println)


    val sourceGraph = GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._
      val src = source.filter(_ % 2 == 0)
      val pipe = builder.add(Flow[Int])
      src ~> pipe.in
      SourceShape(pipe.out)
    }

    Source.fromGraph(sourceGraph).runWith(sink).andThen{case _ => } // sys.terminate()}

    val flowGraph = GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._

      val pipe = builder.add(Flow[Int])
      FlowShape(pipe.in,pipe.out)
    }

    val (_,fut) = Flow.fromGraph(flowGraph).runWith(source,sink)
    fut.andThen{case _ => } //sys.terminate()}


    val sinkGraph = GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._
      val pipe = builder.add(Flow[Int])
      pipe.out.map(_ * 3) ~> Sink.foreach(println)
      SinkShape(pipe.in)
    }

    val fut1 = Sink.fromGraph(sinkGraph).runWith(source)

    Thread.sleep(1000)
    sys.terminate()
  }

}
