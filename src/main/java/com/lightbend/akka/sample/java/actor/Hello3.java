package com.lightbend.akka.sample.java.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Created by fei2 on 2018/5/9.
 * 参考清单6
 * 参考：https://www.ibm.com/developerworks/cn/java/j-jvmc5/
 */
public class Hello3 {
    
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("actor-demo-java");
        ActorRef bob = system.actorOf(Greeter.props("Bob", "Howya doing"));
        ActorRef alice = system.actorOf(Greeter.props("Alice", "Happy to meet you"));
        bob.tell(new Greet(alice), ActorRef.noSender());
        alice.tell(new Greet(bob), ActorRef.noSender());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { /* ignore */ }
        system.terminate();
    }
    
    // messages
    private static class Greet {
        public final ActorRef target;
        
        public Greet(ActorRef actor) {
            target = actor;
        }
    }
    
    private static Object AskName = new Object();
    
    private static class TellName {
        public final String name;
        
        public TellName(String name) {
            this.name = name;
        }
    }
    
    // actor implementation
    private static class Greeter extends UntypedActor {
        private final String myName;
        private final String greeting;
        
        Greeter(String name, String greeting) {
            myName = name;
            this.greeting = greeting;
        }
        
        public static Props props(String name, String greeting) {
            return Props.create(Greeter.class, name, greeting);
        }
        
        public void onReceive(Object message) throws Exception {
            if (message instanceof Greet) {
                ((Greet)message).target.tell(AskName, self());
                
            } else if (message == AskName) {
                //sender();获得最近的消息发送者；if 接受者.tell(message,null);再次利用sender(),方法去寻找sender时，会获得deadLetters；
                sender().tell(new TellName(myName), self());
            } else if (message instanceof TellName) {
                System.out.println(greeting + ", " + ((TellName)message).name);
            }
        }
    }
}
