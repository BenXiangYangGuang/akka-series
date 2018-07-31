import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}

/**
  * Author: fei2
  * Date:  18-7-30 下午1:51
  * Description: 利用工具包的kafka的启动和停止
  * Refer To:
  */


object KafkaEmbedded {

  def start():Unit =
    start(19000)

  def start(kafkaPort:Int): Unit =
    EmbeddedKafka.start()(EmbeddedKafkaConfig(kafkaPort))

  def stop = EmbeddedKafka.stop()

}
