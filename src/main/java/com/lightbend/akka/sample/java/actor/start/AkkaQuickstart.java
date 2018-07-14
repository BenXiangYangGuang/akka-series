package com.lightbend.akka.sample.java.actor.start;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lightbend.akka.sample.java.actor.start.Greeter.*;

import java.io.IOException;

/**
 * Created by fei2 on 2018/4/24.
 */
public class AkkaQuickstart {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("helloakka");
        
        final ActorRef printerActor = system.actorOf(Printer.props(),"printerActor");
        final ActorRef howdyGreeter = system.actorOf(Greeter.props("Howdy",printerActor),"howdyGreeter");
        final ActorRef helloGreeter = system.actorOf(Greeter.props("Hello",printerActor),"helloGreeter");
        final ActorRef goodDayGreeter = system.actorOf(Greeter.props("Good day",printerActor),"goodDayGreeter");
        
        //listener.tell(msg,sender);
        //接受者.tell(msg,发送者);
        
        howdyGreeter.tell(new WhoToGreet("Akka"),ActorRef.noSender());
        howdyGreeter.tell(new Greet(),ActorRef.noSender());
        
        howdyGreeter.tell(new WhoToGreet("Lingtbend"),ActorRef.noSender());
        howdyGreeter.tell(new Greet(),ActorRef.noSender());
        
        helloGreeter.tell(new WhoToGreet("Java"),ActorRef.noSender());
        helloGreeter.tell(new Greet(),ActorRef.noSender());
        
        goodDayGreeter.tell(new WhoToGreet("Play"),ActorRef.noSender());
        goodDayGreeter.tell(new Greet(),ActorRef.noSender());
        
        System.out.println(">>> Press ENTER to exit");
    
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    
    
    }
}
