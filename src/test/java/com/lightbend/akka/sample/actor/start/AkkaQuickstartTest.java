package com.lightbend.akka.sample.actor.start;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.sample.java.actor.start.Greeter;
import com.lightbend.akka.sample.java.actor.start.Greeter.*;
import com.lightbend.akka.sample.java.actor.start.Printer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Created by fei2 on 2018/4/24.
 */
public class AkkaQuickstartTest {
    
    static ActorSystem system;
    
    @BeforeClass
    public static void setup(){
        system = ActorSystem.create();
    }
    
    @AfterClass
    public static void teardown(){
        TestKit.shutdownActorSystem(system);
        system = null;
    }
    
    @Test
    public void testGreeterActorSendingOfGreeting(){
        final TestKit testProbe = new TestKit(system);
        final ActorRef helloGreeter = system.actorOf(Greeter.props("Hello",testProbe.getRef()));
        helloGreeter.tell(new WhoToGreet("Akka"),ActorRef.noSender());
        helloGreeter.tell(new Greet(),ActorRef.noSender());
    
        Printer.Greeting greeting = testProbe.expectMsgClass(Printer.Greeting.class);
        
        assertEquals("Hello,Akka",greeting.message);
    }
    
}
