/*
 * Copyright (C) 2014 - 2016 Softwaremill <http://softwaremill.com>
 * Copyright (C) 2016 - 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package com.wewe.consumer;


import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Pair;
import akka.kafka.*;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import com.typesafe.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 真的有被压策略;根据消费者的能力来拉去不同量的数据;如果消费者能力能够足够大,就能消费很多数据
 * <p>
 * 消费者消费消息语义
 * offSet 在kafka 0.8.2 之前有zookeeper 保存offSet,为了减轻zookeeper 压力,
 * 之后在服务器端添加了一个__consusmer_offsets的内部topic,简称为offset topic;用来保存消费者提交的offset;
 * <p>
 * 1.最简单的offset,提交方式:设置自动提交;enable.auto.commit = true, auto.commit.interval.ms;原理:在每次poll数据之前检测是否需要自动提交;
 * 并提交上一次poll 方法返回的最后一次的offset.为了避免消息丢失,建议在poll方法之前要处理玩上次poll拉取的全部消息.
 * kafkaConsumer 提供了两个手动提交的方法;commitSync()和commitAsync() 同步提交和异步提交
 * <p>
 * delivery guarantee semantic 传递保证语义
 * <p>
 * 1.At most once 最多一次,消息可能丢失,但不会重复传递
 * 2.At least once 至少一次,消息绝不会丢失;的可能会重复传递
 * 3.Exactly once :每条消息只会被传递一次
 * <p>
 * 生产者:
 * 网络原因,未得到kafka的确认消息;会重复发送消息;出现 at least once 情况.
 * 实现exactly once :
 * 1.每一个分区只有一个生产者写入消息;出现异常或者超时,生产者查询这个分区的最后一个消息.用来决定是重新传递还是继续发送消息.
 * 2.未每一个消息添加一个唯一的主键;生产者不对消息进行处理,这部分由消费者来进行去重.
 * 消费者
 * 先消费,在提交offset;存在重复消费问题;AT least once
 * 先提交offset,在消费;存在遗漏消费问题;AT most once
 * exactly once语义，提供一种方案:消费者关闭自动提交offset的功能且不再手动提交offset;有消费者自己在外部保存offset;
 * 提交offset 和事务在一个原子操作中;成功提交offset;否则回滚事务.
 * 当出现宕机或者重启 rebalance时候,从外部查找offset;调用KafkaConsumer.seek()方法手动设置消费位置，消费者从这个offset开始继续消费
 * 消费者并不知道Consumer Group什么时候会发生rebalance操作，哪一个分区分配给哪一个消费者消费。我们可以通过向KafkaConsumer添加ConsumerRebanlanceListener接口来解决这个问题:
 * # onPartitionsRevoked： 消费者停止拉取数据之后，rebalance之前发生，我们可以在此方法中实现手动提交offset，这就避免了rebalance到指定重复消费问题
 * Rebalance之前，可能是消费者A消费者在消费partition-01，Rebalance之后，可能就是消费者B消费者在消费partition-01，
 * 如果rebalance之前没有提交offset，那么消费者就有可能从上一次提交offset的位置开始消费
 * <p>
 * # onPartitionsAssigned: 在rebalance之后，消费者拉取数据之前调用，我们可以在此方法中调整或者定义offset的值
 *
 * 至少一次提交存在下游数据处理存在乱序;offset提交错误问题;因而进行数据的提前接受分流;不同处理的不同produce接受;
 * 从而保证每一个produce存储的数据是有序的.
 *
 */
abstract class ConsumerExample {
    protected final ActorSystem system = ActorSystem.create("example");

    protected final Materializer materializer = ActorMaterializer.create(system);

    protected final int maxPartitions = 100;

    protected <T> Flow<T, T, NotUsed> business() {
        return Flow.create();
    }

    // #settings
    final Config config = system.settings().config().getConfig("akka.kafka.consumer");
    final ConsumerSettings<String, byte[]> consumerSettings =
            ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
                    .withBootstrapServers("localhost:9092")
                    .withGroupId("group2")
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // #settings

    final ConsumerSettings<String, byte[]> consumerSettingsWithAutoCommit =
            // #settings-autocommit
            consumerSettings
                    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                    .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");
    // #settings-autocommit

    protected final ProducerSettings<String, byte[]> producerSettings =
            ProducerSettings.create(system, new StringSerializer(), new ByteArraySerializer())
                    .withBootstrapServers("localhost:9092");


}

// Consume messages and store a representation, including offset, in OffsetStorage

/**
 * Consumer.plainSource  and Consumer.plainPartitionedManualOffsetSource
 * 不支持提交offset 到kafka;可以由一下两种方式保存offset
 * 1.保存offset;到外部系统;数据库自己维护
 * 2.设置自动提交;不是由这个方法来来管理offset;有consumer配置的而是自动提交offset 到 kafka内部;
 * 配置:
 * final ConsumerSettings<String, byte[]> consumerSettingsWithAutoCommit =
 * // #settings-autocommit
 * consumerSettings
 * .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
 * .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");
 */
class ExternalOffsetStorageExample extends ConsumerExample {
    public static void main(String[] args) {
        new ExternalOffsetStorageExample().demo();
    }

    public void demo() {
        // #plainSource
        final OffsetStorage db = new OffsetStorage();

        //“at-least-once” semantics
        CompletionStage<Consumer.Control> controlCompletionStage =
                db.loadOffset().thenApply(fromOffset -> {
                    return Consumer
                            .plainSource(
                                    consumerSettings,
                                    Subscriptions.assignmentWithOffset(
                                            new TopicPartition("topic1", /* partition: */  0),
                                            fromOffset
                                    )
                            )
                            .mapAsync(1, db::businessLogicAndStoreOffset)
                            .to(Sink.ignore())
                            .run(materializer);
                });
        // #plainSource
    }

    // #plainSource


    class OffsetStorage {
        // #plainSource
        private final AtomicLong offsetStore = new AtomicLong();

        // #plainSource
        public CompletionStage<Done> businessLogicAndStoreOffset(ConsumerRecord<String, byte[]> record) { // ... }
            // #plainSource
            System.out.println("--------------接受到数据--------------------");
            System.out.println(record.value());
            System.out.println("--------------接受到数据--------------------");
            offsetStore.set(record.offset());
            return CompletableFuture.completedFuture(Done.getInstance());
        }

        // #plainSource
        public CompletionStage<Long> loadOffset() { // ... }
            // #plainSource

            return CompletableFuture.completedFuture(offsetStore.get());
        }

        // #plainSource
    }
    // #plainSource

}

// Consume messages at-most-once
class AtMostOnceExample extends ConsumerExample {
    public static void main(String[] args) {
        new AtMostOnceExample().demo();
    }


    public void demo() {
        //atMostOnceSource commits the offset for each message and that is rather slow, batching of commits is recommended.
        //他会管理自动提交也不是  由 ENABLE_AUTO_COMMIT_CONFIG 这个配置来控制提交的;他也支持批处理提交
        //每次提交信息比较慢,建议使用批处理提交
        // #atMostOnce
        Consumer.Control control =
                Consumer
                        .atMostOnceSource(consumerSettings, Subscriptions.topics("topic1"))
                        //并行度为10,并不影响数据的处理顺序,还是按照原来的数据顺序进行计算后的数据返回
                        .mapAsync(10, record -> business(record.key(), record.value()))
                        .to(Sink.foreach(it -> System.out.println("Done with " + it)))
                        .run(materializer);

        // #atMostOnce
    }

    // #atMostOnce
    CompletionStage<String> business(String key, byte[] value) { // .... }
        // #atMostOnce
        String val = new String(value);
        if (Integer.valueOf(val) % 4 < 2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return CompletableFuture.completedFuture(val);
    }
}

// Consume messages at-least-once
class AtLeastOnceExample extends ConsumerExample {
    public static void main(String[] args) {
        new AtLeastOnceExample().demo();
    }

    public void demo() {
        // #atLeastOnce
        Consumer.Control control =
                Consumer
                        .committableSource(consumerSettings, Subscriptions.topics("topic1"))
                        .mapAsync(1, msg ->
                                business(msg.record().key(), msg.record().value())
                                        .thenApply(done -> msg.committableOffset()))
                        .mapAsync(1, offset -> offset.commitJavadsl())  //提交offset 动作
                        .to(Sink.ignore())
                        .run(materializer);
        // #atLeastOnce
    }

    // #atLeastOnce

    CompletionStage<String> business(String key, byte[] value) { // .... }
        // #atLeastOnce
        return CompletableFuture.completedFuture(new String(value));
    }

}

// Consume messages at-least-once, and commit in batches
class AtLeastOnceWithBatchCommitExample extends ConsumerExample {
    public static void main(String[] args) {
        new AtLeastOnceWithBatchCommitExample().demo();
    }

    //Note that it will only aggregate elements into batches if the downstream consumer is slower than the upstream producer.
    //只有下游的Consumer消费速率慢于producter,才会进行聚合到batch;
    public void demo() {
        // #atLeastOnceBatch
        Consumer.Control control =
                Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
                        .mapAsync(1, msg ->
                                business(msg.record().key(), msg.record().value())
                                        .thenApply(done -> msg.committableOffset())
                        )
                        .batch(
                                20,
                                ConsumerMessage::createCommittableOffsetBatch,  //从第一个位置开始创建一个存储offset的bat
                                ConsumerMessage.CommittableOffsetBatch::updated  //添加或更新一个offset到对应的topic 组,分区
                        )
                        .mapAsync(3, c -> c.commitJavadsl())
                        .to(Sink.ignore())
                        .run(materializer);
        // #atLeastOnceBatch
    }

    CompletionStage<String> business(String key, byte[] value) { // .... }
        return CompletableFuture.completedFuture("");
    }
}

// Connect a Consumer to Producer
class ConsumerToProducerSinkExample extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerToProducerSinkExample().demo();
  }

  public void demo() {
      // #consumerToProducerSink
      // 重点在于一个commitableSink,创建了一个背压式的 sink; 发送数据使用 "at least once" 语义
      // 在第一个map之前做数据的操作,而后创建sink 进行数据的发送
      // 接受两个topic的数据,是交替行的.
      // +PassThrough 转移量 就是一个ConsumerMessage.Committable,记录Consumer offset位置,为了后续操作方便;
    Consumer.Control control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1", "topic2"))
          .map(msg ->
              new ProducerMessage.Message<String, byte[], ConsumerMessage.Committable>(
                  new ProducerRecord<>("targetTopic", msg.record().key(), msg.record().value()),
                  msg.committableOffset()
              )
          )
          .to(Producer.commitableSink(producerSettings))
          .run(materializer);
    // #consumerToProducerSink
    control.shutdown();
  }
}

// Connect a Consumer to Producer
class ConsumerToProducerFlowExample extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerToProducerFlowExample().demo();
  }

  public void demo() {
      // #consumerToProducerFlow
      Consumer.DrainingControl<Done> control =
          Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
              .map(msg -> {
                ProducerMessage.Envelope<String, byte[], ConsumerMessage.Committable> prodMsg =
                    new ProducerMessage.Message<>(
                        new ProducerRecord<>("topic2", msg.record().value()),
                        msg.committableOffset() // the passThrough
                    );
                return prodMsg;
              })

              .via(Producer.flexiFlow(producerSettings))

              .mapAsync(producerSettings.parallelism(), result -> {
                  ConsumerMessage.Committable committable = result.passThrough();
                  return committable.commitJavadsl();
              })
              .toMat(Sink.ignore(), Keep.both())
              .mapMaterializedValue(Consumer::createDrainingControl)
              .run(materializer);
      // #consumerToProducerFlow
  }
}

// Connect a Consumer to Producer, and commit in batches
class ConsumerToProducerWithBatchCommitsExample extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerToProducerWithBatchCommitsExample().demo();
  }

  public void demo() {
    // consumerToProducerFlowBatch 进行批量提交
      // Consumer.Control 相当于 Materialized value of the consumer `Source`
    Source<ConsumerMessage.CommittableOffset, Consumer.Control> source =
      Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
      .map(msg -> {
          ProducerMessage.Envelope<String, byte[], ConsumerMessage.CommittableOffset> prodMsg =
              new ProducerMessage.Message<>(
                  new ProducerRecord<>("topic2", msg.record().value()),
                  msg.committableOffset()
              );
          return prodMsg;
      })
      .via(Producer.flexiFlow(producerSettings))
      .map(result -> result.passThrough());

    source
        .batch(
          20,
          ConsumerMessage::createCommittableOffsetBatch,
          ConsumerMessage.CommittableOffsetBatch::updated
        )
        .mapAsync(3, c -> c.commitJavadsl())
        .runWith(Sink.ignore(), materializer);
    // #consumerToProducerFlowBatch
  }
}

// Connect a Consumer to Producer, and commit in batches // #groupedWithin
class ConsumerToProducerWithBatchCommits2Example extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerToProducerWithBatchCommits2Example().demo();
  }

  public void demo() {
    Source<ConsumerMessage.CommittableOffset, Consumer.Control> source =
      Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
      .map(msg -> {
          ProducerMessage.Envelope<String, byte[], ConsumerMessage.CommittableOffset> prodMsg =
              new ProducerMessage.Message<>(
                  new ProducerRecord<>("topic2", msg.record().value()),
                  msg.committableOffset()
              );
          return prodMsg;
      })
      .via(Producer.flexiFlow(producerSettings))
      .map(result -> result.passThrough());

      // #groupedWithin
      source
        .groupedWithin(20, java.time.Duration.of(5, ChronoUnit.SECONDS))
        .map(ConsumerMessage::createCommittableOffsetBatch)
        .mapAsync(3, c -> c.commitJavadsl())
      // #groupedWithin
        .runWith(Sink.ignore(), materializer);
  }
}

//一个数据源一个分区;committablePartitionedSource 支持自动分配kafka的topic-partition
//topic-partition 取消之后,相应的source完成.
//每个分区是被压式的
// Backpressure per partition with batch commit
class ConsumerWithPerPartitionBackpressure extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerWithPerPartitionBackpressure().demo();
  }

  public void demo() {
      //线程池
    final Executor ec = Executors.newCachedThreadPool();
    // #committablePartitionedSource
    Consumer.DrainingControl<Done> control =
        Consumer
            .committablePartitionedSource(consumerSettings, Subscriptions.topics("topic1"))
            .flatMapMerge(maxPartitions, Pair::second)  //数据流并行maxPartitions份,最终合并到一份.处理后的数据是无须的.Pair::second 指的是上一步完成结果的第二个参数
            .via(business())
            .map(msg -> msg.committableOffset())
            .batch(
                100,
                ConsumerMessage::createCommittableOffsetBatch,
                ConsumerMessage.CommittableOffsetBatch::updated
            )
            .mapAsync(3, offsets -> offsets.commitJavadsl())
            .toMat(Sink.ignore(), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)  //创建Consumer.DrainingControl<Done> control
            .run(materializer);
    // #committablePartitionedSource
    control.drainAndShutdown(ec);
  }
}

class ConsumerWithIndependentFlowsPerPartition extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerWithIndependentFlowsPerPartition().demo();
  }

  public void demo() {
    final Executor ec = Executors.newCachedThreadPool();
    // #committablePartitionedSource-stream-per-partition
    Consumer.DrainingControl<Done> control =
        Consumer
            .committablePartitionedSource(consumerSettings, Subscriptions.topics("topic1"))
            .map(pair -> {
                Source<ConsumerMessage.CommittableMessage<String, byte[]>, NotUsed> source = pair.second();
                return source
                    .via(business())
                    .mapAsync(1, message -> message.committableOffset().commitJavadsl())
                    .runWith(Sink.ignore(), materializer);
            })
            .mapAsyncUnordered(maxPartitions, completion -> completion)
            .toMat(Sink.ignore(), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)
            .run(materializer);
    // #committablePartitionedSource-stream-per-partition
    control.drainAndShutdown(ec);
  }
}

class ExternallyControlledKafkaConsumer extends ConsumerExample {
  public static void main(String[] args) {
    new ExternallyControlledKafkaConsumer().demo();
  }

  public void demo() {
    ActorRef self = system.deadLetters();
    // #consumerActor
    //Consumer is represented by actor
    ActorRef consumer = system.actorOf((KafkaConsumerActor.props(consumerSettings)));

    //Manually assign topic partition to it
    Consumer.Control controlPartition1 = Consumer
        .plainExternalSource(
            consumer,
            Subscriptions.assignment(new TopicPartition("topic1", 1))
        )
        .via(business())
        .to(Sink.ignore())
        .run(materializer);

    //Manually assign another topic partition
    Consumer.Control controlPartition2 = Consumer
        .plainExternalSource(
            consumer,
            Subscriptions.assignment(new TopicPartition("topic1", 2))
        )
        .via(business())
        .to(Sink.ignore())
        .run(materializer);

    consumer.tell(KafkaConsumerActor.stop(), self);
    // #consumerActor
  }
}
    //createNoopControl() 版本不对应; 缺少方法
/*class RestartingConsumer extends ConsumerExample {
  public static void main(String[] args) { new RestartingConsumer().demo(); }

  public void demo() {
    //#restartSource
    AtomicReference<Consumer.Control> control = new AtomicReference<>(Consumer.createNoopControl());

    RestartSource.onFailuresWithBackoff(
        java.time.Duration.ofSeconds(3),
        java.time.Duration.ofSeconds(30),
        0.2,
        () ->
            Consumer
              .plainSource(consumerSettings, Subscriptions.topics("topic1"))
              .mapMaterializedValue(c -> { control.set(c); return c; })
              .via(business())
    )
    .runWith(Sink.ignore(), materializer);

    control.get().shutdown();
    //#restartSource
  }
}*/

class RebalanceListenerCallbacksExample extends ConsumerExample {
  public static void main(String[] args) {
    new ExternallyControlledKafkaConsumer().demo();
  }

  // #withRebalanceListenerActor
  class RebalanceListener extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
      return receiveBuilder()
          .match(TopicPartitionsAssigned.class, assigned -> {
            log().info("Assigned: {}", assigned);
          })
          .match(TopicPartitionsRevoked.class, revoked -> {
            log().info("Revoked: {}", revoked);
          })
          .build();
    }
  }

  // #withRebalanceListenerActor

  public void demo(ActorSystem system) {
    // #withRebalanceListenerActor
    ActorRef rebalanceListener = this.system.actorOf(Props.create(RebalanceListener.class));

    Subscription subscription = Subscriptions.topics("topic")
        // additionally, pass the actor reference:
        .withRebalanceListener(rebalanceListener);

    // use the subscription as usual:
    Consumer
      .plainSource(consumerSettings, subscription);
    // #withRebalanceListenerActor
  }

}

class ConsumerMetricsExample extends ConsumerExample {
  public static void main(String[] args) {
    new ConsumerMetricsExample().demo();
  }

  public void demo() {
    // #consumerMetrics
    // run the stream to obtain the materialized Control value
    Consumer.Control control = Consumer
        .plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition("topic1", 0)))
        .via(business())
        .to(Sink.ignore())
        .run(materializer);

    CompletionStage<Map<MetricName, Metric>> metrics = control.getMetrics();
    metrics.thenAccept(map -> System.out.println("Metrics: " + map));
    // #consumerMetrics
  }
}

// Shutdown via Consumer.Control
class ShutdownPlainSourceExample extends ConsumerExample {
  public static void main(String[] args) {
    new ExternalOffsetStorageExample().demo();
  }

  public void demo() {
    // #shutdownPlainSource
    final OffsetStorage db = new OffsetStorage();

    db.loadOffset().thenAccept(fromOffset -> {
      Consumer.Control control = Consumer
          .plainSource(
              consumerSettings,
              Subscriptions.assignmentWithOffset(new TopicPartition("topic1", 0), fromOffset)
          )
          .mapAsync(10, record -> {
              return business(record.key(), record.value())
                  .thenApply(res -> db.storeProcessedOffset(record.offset()));
          })
          .toMat(Sink.ignore(), Keep.left())
          .run(materializer);

      // Shutdown the consumer when desired
      control.shutdown();
    });
    // #shutdownPlainSource
  }

  CompletionStage<String> business(String key, byte[] value) { // .... }
    return CompletableFuture.completedFuture("");
  }

  class OffsetStorage {

    private final AtomicLong offsetStore = new AtomicLong();

    public CompletionStage<Done> storeProcessedOffset(long offset) { // ... }
      offsetStore.set(offset);
      return CompletableFuture.completedFuture(Done.getInstance());
    }

    public CompletionStage<Long> loadOffset() { // ... }
      return CompletableFuture.completedFuture(offsetStore.get());
    }

  }

}


/**
 * Consumer.DrainingControl  drainAndShutdown 停止流程
 * 1.Consumer.Control.stop() to stop producing messages from the Source. This does not stop the underlying Kafka Consumer
 * 2.Wait for the stream to complete,
 * so that a commit request has been made for all offsets of all processed messages (via commitScaladsl() or commitJavadsl()).
 * 3.Consumer.Control.shutdown() to wait for all outstanding commit requests to finish and stop the Kafka Consumer.
 */
// Shutdown when batching commits
class ShutdownCommittableSourceExample extends ConsumerExample {
  public static void main(String[] args) {
    new AtLeastOnceExample().demo();
  }

  public void demo() {
    // #shutdownCommitableSource
    final Executor ec = Executors.newCachedThreadPool();

    Consumer.DrainingControl<Done> control =
        Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
            .mapAsync(1, msg ->
                business(msg.record().key(), msg.record().value()).thenApply(done -> msg.committableOffset()))
            .batch(20,
                first -> ConsumerMessage.createCommittableOffsetBatch(first),
                (batch, elem) -> batch.updated(elem)
            )
            .mapAsync(3, c -> c.commitJavadsl())
            .toMat(Sink.ignore(), Keep.both())
            .mapMaterializedValue(Consumer::createDrainingControl)
            .run(materializer);

    control.drainAndShutdown(ec);
    // #shutdownCommitableSource
  }

  CompletionStage<String> business(String key, byte[] value) { // .... }
    return CompletableFuture.completedFuture("");
  }

}


