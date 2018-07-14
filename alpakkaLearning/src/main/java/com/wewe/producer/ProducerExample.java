package com.wewe.producer;

import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * @Author: fei2
 * @Date: 18-7-10 上午11:15
 * @Description:
 * @Refer To:
 */
abstract class ProducerExample {

    protected final ActorSystem system = ActorSystem.create("example");

    protected final Materializer materializer = ActorMaterializer.create(system);

    final Config config = system.settings().config().getConfig("akka.kafka.producer");

    final ProducerSettings<String, String> producerSettings =
            ProducerSettings
                    .create(config, new StringSerializer(), new StringSerializer())
                    .withBootstrapServers("localhost:9092");

    final KafkaProducer<String, String> kafkaProducer =
            producerSettings.createKafkaProducer();
}
