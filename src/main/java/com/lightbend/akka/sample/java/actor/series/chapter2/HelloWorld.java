package com.lightbend.akka.sample.java.actor.series.chapter2;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Created by fei2 on 2018/5/11.
 */
public class HelloWorld extends UntypedActor {
    
    @Override
    public void preStart(){
            final ActorRef greeter = getContext().actorOf(Props.create(Greeter.class));
            greeter.tell(Greeter.Msg.GREET,self());
            System.out.println(greeter.isTerminated());
            System.out.println("greeter发送消息是否存活"+greeter.path());
       
    }
    
    @Override
    public void onReceive(Object message) throws Throwable {
        if(message == Greeter.Msg.DONE){
            // when the greeter is done, stop this actor and with it the application
            System.out.println("-------------接受到done消息-------------------");
            getContext().stop(getSelf());
        }else{
            unhandled(message);
        }
    }
}
