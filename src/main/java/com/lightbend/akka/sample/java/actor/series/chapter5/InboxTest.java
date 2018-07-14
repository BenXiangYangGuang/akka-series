package com.lightbend.akka.sample.java.actor.series.chapter5;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by fei2 on 2018/5/12.
 * 信箱直接接受message
 */
public class InboxTest extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(),this);
    
    public enum Msg{
        WORKING,DONE,CLOSE
    }
    
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message == Msg.WORKING){
            logger.info("i am working");
        }else if( message == Msg.DONE){
            logger.info("i am done");
        }else if(message == Msg.CLOSE){
            logger.info("i am close");
            getSender().tell(Msg.CLOSE,getSelf());
            getContext().stop(getSelf());
        }else {
            unhandled(message);
        }
    }
    
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("index", ConfigFactory.load("akka.conf"));
        ActorRef inboxTest = system.actorOf(Props.create(InboxTest.class),"InboxTest");
    
        Inbox inbox = Inbox.create(system);
        inbox.watch(inboxTest);     //监听一个actor
        
        inbox.send(inboxTest, Msg.WORKING);
        inbox.send(inboxTest, Msg.DONE);
        inbox.send(inboxTest, Msg.CLOSE);
        
        while (true){
            try {
                Object receive = inbox.receive(Duration.create(1, TimeUnit.SECONDS));
                if(receive == Msg.CLOSE){
                    System.out.println("inboxTextActor is closing");
                }else if (receive instanceof Terminated) { //中断和线程一个概念
                    System.out.println("inboxTextActor is closed");
                    system.terminate();
                    break;
                }
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
}
