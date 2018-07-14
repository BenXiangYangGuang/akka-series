package com.lightbend.akka.sample.java.actor.series.chapter2;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * Created by fei2 on 2018/5/11.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("Hello");
        ActorRef a = system.actorOf(Props.create(HelloWorld.class), "helloWorld");
        Thread.sleep(1000);
        system.terminate();
    }

}
