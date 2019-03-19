package com.wewe;

import akka.actor.*;
import akka.dispatch.OnSuccess;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static akka.pattern.Patterns.ask;
import static com.wewe.TransformationMessages.BACKEND_REGISTRATION;

/**
 * @Author: fei2
 * @Date: 18-7-9 上午11:17
 * @Description: 发送 TransformationMessages 消息,服务器端收到消息,转化为大写,并返回大写消息
 * @Refer To:
 */
public class MyAkkaClusterClient extends UntypedActor {


    //每一个客户端注册过得服务端容器
    List<ActorRef> backends = new ArrayList<ActorRef>();
    int jobCounter = 0;

    @Override
    public void onReceive(Object message) {
        if ((message instanceof TransformationMessages.TransformationJob) && backends.isEmpty()) {//无服务提供者
            TransformationMessages.TransformationJob job = (TransformationMessages.TransformationJob) message;
            getSender().tell(
                    new TransformationMessages.JobFailed("Service unavailable, try again later", job),
                    getSender());

        } else if (message instanceof TransformationMessages.TransformationJob) {

            //接收到由client端 main函数发送的消息;在发送到服务器端消息
            TransformationMessages.TransformationJob job = (TransformationMessages.TransformationJob) message;
            /**
             * 这里在客户端业务代码里进行负载均衡操作。实际业务中可以提供多种负载均衡策略，并且也可以做分流限流等各种控制。
             */
            jobCounter++;
            backends.get(jobCounter % backends.size())
                    .forward(job, getContext());

        } else if (message.equals(BACKEND_REGISTRATION)) {
            /**注册服务提供者
             *
             */
            //watch 操作用于注册这个getSender()(ActorRef)，作为一个监控器；当检测到服务器（getSender()）这个节点terminated;这个actor(本类actor)，将会收到一个terminated消息；
            getContext().watch(getSender());//这里对服务提供者进行watch
            backends.add(getSender());

        } else if (message instanceof Terminated) {
            /**
             * 移除服务提供者
             */
            Terminated terminated = (Terminated) message;
            backends.remove(terminated.getActor());

        } else {
            unhandled(message);
        }
    }

    public static void main(String [] args){
        System.out.println("Start myAkkaClusterClient");
        ActorSystem actorSystem = ActorSystem.create("akkaClusterTest", ConfigFactory.load("client.conf"));
        final ActorRef myAkkaClusterClient = actorSystem.actorOf(Props.create(MyAkkaClusterClient.class), "myAkkaClusterClient");
        System.out.println("Started myAkkaClusterClient");

        final FiniteDuration interval = Duration.create(2, TimeUnit.SECONDS);
        final Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        final ExecutionContext ec = actorSystem.dispatcher();
        final AtomicInteger counter = new AtomicInteger();

        actorSystem.scheduler().schedule(interval, interval, new Runnable() {
            //向客户端发送消息
            public void run() {
                ask(myAkkaClusterClient, new TransformationMessages.TransformationJob("hello-" + counter.incrementAndGet()), timeout)
                        .onSuccess(new OnSuccess<Object>() {
                            public void onSuccess(Object result) {
                                System.out.println(result.toString());
                            }
                        }, ec);
            }
        }, ec);

    }
}
