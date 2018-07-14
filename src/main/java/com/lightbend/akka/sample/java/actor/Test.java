package com.lightbend.akka.sample.java.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fei2 on 2018/5/10.
 */
public class Test {
    
    private static Map<String,ActorRef> map = new HashMap<>();
    
    static class Greet extends UntypedActor{
        
        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof String){
                System.out.println("------------接受到消息----------------");
            }
        }
    }
    
    public static void main(String[] args) {
        String id = "123";
        
        final ActorSystem system = ActorSystem.create("mySystem");
        ActorRef greetActor = system.actorOf(Props.create(Greet.class));
//        map.put(id,greetActor);
//        ActorRef greet2Actor = map.get(id);
        greetActor.tell(new String(),ActorRef.noSender());
        system.terminate();
    }
}
