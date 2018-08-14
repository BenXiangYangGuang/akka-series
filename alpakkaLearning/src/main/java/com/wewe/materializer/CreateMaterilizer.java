package com.wewe.materializer;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Author: fei2
 * Date:  18-8-3 下午6:40
 * Description:测试创建 materializer
 * Refer To:
 */
public class CreateMaterilizer {

    public static void main(String[] args) {
        String AKKA_CONF_FILE_NAME = "actor-system.conf";
        String ACTOR_SYSTEM_NAME = "example";
        Config config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
        ActorSystem system = ActorSystem.create(ACTOR_SYSTEM_NAME, config);
        Materializer materializer = ActorMaterializer.create(system);

        ConsumerSettings<String, byte[]> consumerSettings =
                ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers("localhost:9092")
                        .withGroupId("group1")
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                        .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");
        Consumer.Control control = Consumer.atMostOnceSource(consumerSettings, Subscriptions.topics("topic1"))
                //.map(msg -> msg.toString())
                .to(Sink.foreach(it -> System.out.println("SSSSSS" + new String(it.value()))))
                .run(materializer);

    }

}
