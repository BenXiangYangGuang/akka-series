package com.wewe.producer;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @Author: fei2
 * @Date: 18-7-10 上午11:15
 * @Description: alpakka-kafka-connector quick-start
 * @Refer To:
 */

abstract class ProducerExample {
    protected final ActorSystem system = ActorSystem.create("example");

    protected final Materializer materializer = ActorMaterializer.create(system);

    // #producer
    // #settings
    final Config config = system.settings().config().getConfig("akka.kafka.producer");
    final ProducerSettings<String, String> producerSettings =
            ProducerSettings
                    .create(config, new StringSerializer(), new StringSerializer())
                    .withBootstrapServers("localhost:9092");
    // #settings
    final KafkaProducer<String, String> kafkaProducer =
            producerSettings.createKafkaProducer();
    // #producer

    protected void terminateWhenDone(CompletionStage<Done> result) {
        result
                .exceptionally(e -> {
                    system.log().error(e, e.getMessage());
                    return Done.getInstance();
                })
                .thenAccept(d -> system.terminate());
    }
}
//一个最简单的生产者的sink;自带背压策略
class PlainSinkExample extends ProducerExample {
    public static void main(String[] args) {
        new PlainSinkExample().demo();
    }

    public void demo() {
        // #plainSink
        CompletionStage<Done> done =
                Source.range(1, 100)
                        .map(number -> number.toString())
                        .map(value -> new ProducerRecord<String, String>("topic1", value))
                        .runWith(Producer.plainSink(producerSettings), materializer);
        // #plainSink

        terminateWhenDone(done);
    }
}
//多了一个kafkaProduce参数的最简单的生产者的sink;自带背压策略
class PlainSinkWithProducerExample extends ProducerExample {
    public static void main(String[] args) {
        new PlainSinkExample().demo();
    }

    public void demo() {
        // #plainSinkWithProducer
        CompletionStage<Done> done =
                Source.range(1, 100)
                        .map(number -> number.toString())
                        .map(value -> new ProducerRecord<String, String>("topic1", value))
                        .runWith(Producer.plainSink(producerSettings, kafkaProducer), materializer);
        // #plainSinkWithProducer

        terminateWhenDone(done);
    }
}
//监视器
class ObserveMetricsExample extends ProducerExample {
    public static void main(String[] args) {
        new PlainSinkExample().demo();
    }

    public void demo() {
        // #producerMetrics
        Map<org.apache.kafka.common.MetricName, ? extends org.apache.kafka.common.Metric> metrics =
                kafkaProducer.metrics();// observe metrics
        // #producerMetrics
    }
}
//Sinks and flows accept ProducerMessage.Envelope 的实现作为输入;Envelope定义了对简单的passThrough;
//passThrough 可以是一个ConsumerMessage.CommittableOffset;记录了消费记录的变量;为了后续的操作简便
//The ProducerMessage.PassThroughMessage allows to let an element pass through a Kafka flow without producing a new message to a Kafka topic.
// This is primarily useful with Kafka commit offsets and transactions, so that these can be committed without producing new messages.
class ProducerFlowExample extends ProducerExample {
    public static void main(String[] args) {
        new ProducerFlowExample().demo();
    }

    <KeyType, ValueType, PassThroughType> ProducerMessage.Message<KeyType, ValueType, PassThroughType> createMessage(KeyType key, ValueType value, PassThroughType passThrough) {
        return
                // #singleMessage
                new ProducerMessage.Message<KeyType, ValueType, PassThroughType>(
                        new ProducerRecord<>("topicName", key, value),
                        passThrough
                );
        // #singleMessage

    }
    //MultiMessage 就是多条记录
    <KeyType, ValueType, PassThroughType> ProducerMessage.MultiMessage<KeyType, ValueType, PassThroughType> createMultiMessage(KeyType key, ValueType value, PassThroughType passThrough) {
        return
                // #multiMessage
                new ProducerMessage.MultiMessage<KeyType, ValueType, PassThroughType>(
                        Arrays.asList(
                                new ProducerRecord<>("topicName", key, value),
                                new ProducerRecord<>("anotherTopic", key, value)
                        ),
                        passThrough
                );
        // #multiMessage

    }

    <KeyType, ValueType, PassThroughType> ProducerMessage.PassThroughMessage<KeyType, ValueType, PassThroughType> createPassThroughMessage(KeyType key, ValueType value, PassThroughType passThrough) {

        ProducerMessage.PassThroughMessage<KeyType, ValueType, PassThroughType> ptm =
                // #passThroughMessage
                new ProducerMessage.PassThroughMessage<>(
                        passThrough
                );
        // #passThroughMessage
        return ptm;
    }
    //For flows the ProducerMessage.Messages continue as ProducerMessage.Result elements containing:
    //  1.the original input message,
    //  2.the record metadata (Kafka RecordMetadata API), and
    //  3.access to the passThrough within the message.
    public void demo() {
        // #flow
        CompletionStage<Done> done =
                Source.range(1, 100)
                        .map(number -> {
                            int partition = 0;
                            String value = String.valueOf(number);
                            //单条数据
                            /*ProducerMessage.Envelope<String, String, Integer> msg =
                                    new ProducerMessage.Message<String, String, Integer>(
                                            new ProducerRecord<>("topic1", partition, "key", value),
                                            number
                                    );*/
                            //多条数据
                            ProducerMessage.Envelope<String, String, Integer> msg =
                            new ProducerMessage.MultiMessage<String, String, Integer>(
                                    Arrays.asList(
                                            new ProducerRecord<>("topicName",partition, "key", value),
                                            new ProducerRecord<>("anotherTopic", partition,"key", value)
                                    ),
                                    number
                            );
                            return msg;
                        })

                        .via(Producer.flexiFlow(producerSettings))

                        .map(result -> {
                            if (result instanceof ProducerMessage.Result) {
                                ProducerMessage.Result<String, String, Integer> res = (ProducerMessage.Result<String, String, Integer>) result;
                                ProducerRecord<String, String> record = res.message().record();
                                RecordMetadata meta = res.metadata();
                                return meta.topic() + "/" + meta.partition() + " " + res.offset() + ": " + record.value();
                            } else if (result instanceof ProducerMessage.MultiResult) {
                                ProducerMessage.MultiResult<String, String, Integer> res = (ProducerMessage.MultiResult<String, String, Integer>) result;
                                //map把每一个topic分开处理;reduce把分开处理额结果进行合并
                                //Optional[topicName/0 700: 1, anotherTopic/0 700: 1]
                                //acct代表topicName的数据集合;s代表anotherTopic的数据集合
                                return res.getParts().stream().map( part -> {
                                    RecordMetadata meta = part.metadata();
                                    return meta.topic() + "/" + meta.partition() + " " + part.metadata().offset() + ": " + part.record().value();
                                }).reduce((acc, s) -> acc + ", " + s);
                            } else {
                                return "passed through";
                            }
                        })
                        .runWith(Sink.foreach(System.out::println), materializer);
        // #flow

        terminateWhenDone(done);
    }
}