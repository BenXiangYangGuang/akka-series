import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, DurationInt}

/**
  * Author: fei2
  * Date:  18-7-30 上午11:14
  * Description:获取ActorSystem
  * Refer To:
  */
class ActorSystemAvailable {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val actorMaterializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  def wait(duration: Duration): Unit = Thread.sleep(duration.toMillis)

  def terminateActorSystem():Unit = Await.result(actorSystem.terminate(),1.seconds)

}
