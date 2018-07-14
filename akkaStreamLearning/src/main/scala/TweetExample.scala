
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

/**
  * Author: fei2
  * Date: 18-7-11 下午5:03
  * Description:推特博文分析
  * Refer To:
  */
object TweetExample {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("reactive-tweets")
    implicit val materializer = ActorMaterializer()

    val akkaTag = Hashtag("#akka")

    val tweets: Source[Tweet,NotUsed] = Source(
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
      Nil
    )

    tweets
      .map(_.hashtags) // Get all sets of hashtags ...
      .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
      .mapConcat(identity) // Flatten the stream of tweets to a stream of hashtags
      .map(_.name.toUpperCase) // Convert all hashtags to upper case
      .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags
  }


  final case class Author(handle: String)

  //标签
  final case class Hashtag(name: String)

  final case class Tweet(author: Author,timestamp: Long,body: String){
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]",""))
    }.toSet
  }



}
