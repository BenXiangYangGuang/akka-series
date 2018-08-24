/**
  * Author: fei2
  * Date:  18-7-30 上午10:44
  * Description:每30秒通过http请求拉取csv 数据转化为 json 发送到 kafka
  * Refer To:https://developer.lightbend.com/docs/alpakka/current/examples/csv-samples.html
  */
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaRanges}
import akka.http.scaladsl.model.headers.Accept
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter}

import scala.concurrent.duration.DurationInt

object FetchHttpEvery30SecondsAndConvertCsvToJsonToKafka
  extends ActorSystemAvailable
    with App
    with DefaultJsonProtocol{

  val httpRequest = HttpRequest(uri = "https://www.nasdaq.com/screening/companies-by-name.aspx?exchange=NASDAQ&render=download")
    .withHeaders(Accept(MediaRanges.`text/*`))
  //抽取请求中的数据
  def extractEntityData(response: HttpResponse) :Source[ByteString, _] =
    response match {

      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new  RuntimeException(s"illegal response $notOkResponse"))
  }
  //非null判断，然后执行非null 的byteString => String
  def cleanseCsvData(csvData:Map[String,ByteString]):Map[String, String] =
    csvData
      .filterNot { case (key, _) => key.isEmpty }
      .mapValues(_.utf8String)

  def toJson(map: Map[String, String])(
            implicit jsWriter: JsonWriter[Map[String,String]]): JsValue = jsWriter.write(map)

  val kafkaPort = 9092
  KafkaEmbedded.start(kafkaPort)
  //akka.kafka 下的ProducerSettings
  val kafkaProducerSettings = ProducerSettings(actorSystem,new StringSerializer,new StringSerializer)
    .withBootstrapServers(s"localhost:$kafkaPort")

  val (ticks, future) =
    Source
      .tick(1.seconds,30.seconds,httpRequest)
    .mapAsync(1)(Http().singleRequest(_))
    .flatMapConcat(extractEntityData)
    .via(CsvParsing.lineScanner())
    .via(CsvToMap.toMap())
    .map(cleanseCsvData)
    .map(toJson)
    .map(_.compactPrint)
    .map{
      elem =>
        new ProducerRecord[String,String]("topic1",elem)
    }
    .toMat(Producer.plainSink(kafkaProducerSettings))(Keep.both)
    .run()

  val kafkaConsumerSettings = ConsumerSettings(actorSystem,new StringDeserializer,new StringDeserializer)
    .withBootstrapServers(s"localhost:$kafkaPort")
    .withGroupId("topicp1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest")

  val control = Consumer
    .atMostOnceSource(kafkaConsumerSettings,Subscriptions.topics("topic1"))
    .map(_.value())
    .toMat(Sink.foreach(println))(Keep.both)
    .mapMaterializedValue(Consumer.DrainingControl.apply)
    .run()

  wait(1.minute)
  ticks.cancel()

  for{
    _ <- future
    _ <- control.drainAndShutdown()
  } {
    KafkaEmbedded.stop
    terminateActorSystem()
  }

}
