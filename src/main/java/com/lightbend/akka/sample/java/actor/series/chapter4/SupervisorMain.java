package com.lightbend.akka.sample.java.actor.series.chapter4;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

/**
 * Created by fei2 on 2018/5/12.
 */
public class SupervisorMain {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("strategy", ConfigFactory.load("akka.config"));
        ActorRef superVisor = system.actorOf(Props.create(SuperVisor.class), "SuperVisor");
        superVisor.tell(Props.create(RestartActor.class), ActorRef.noSender());
        
        ActorSelection actorSelection = system.actorSelection("akka://strategy/user/SuperVisor/restartActor");//这是akka的路径。restartActor是在SuperVisor中创建的。
        
        for (int i = 0; i < 100; i++) {
            actorSelection.tell(RestartActor.Msg.RESTART, ActorRef.noSender());
        }
    }
}
