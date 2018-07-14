package com.lightbend.akka.sample.java.actor.creator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Created by fei2 on 2018/4/27.
 *
 * 创建父子级actor
 */
public class OtherCreateTest {
    
    enum MSG {
        HI, OK
    }
    
    static class Actor1 extends UntypedActor {
        
        @Override
        public void onReceive(Object message) throws Exception {
            // TODO Auto-generated method stub
            if (message instanceof MSG) {
                if (message.equals(MSG.OK)) {
                    System.out.println(" actor1 i receive ok");
                } else {
                    unhandled(message);
                }
            } else {
                unhandled(message);
            }
            
        }
        @Override
        public void preStart(){
    
            final ActorRef actor2 = getContext().actorOf(Props.create(Actor2.class), "actor2");
    
            actor2.tell(MSG.HI, getSelf());
    
            System.out.println(getSelf().path().parent());
            System.out.println(getSelf().path());
            System.out.println(actor2.path());
    
        }
        
    }
    
    static class Actor2 extends UntypedActor {
        
        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof MSG) {
                if (message.equals(MSG.HI)) {
                    System.out.println(" actor2 i receive hi");
//                    getSender().tell(MSG.OK, getSelf());
                } else {
                    unhandled(message);
                }
            } else {
                unhandled(message);
            }
        }
        
    }

    
    public static void communication(){
        
        // 使用Props创建Actor
        final ActorSystem system = ActorSystem.create("MySystem");
        final ActorRef actor1 = system.actorOf(Props.create(Actor1.class),"actor1");
    
        system.terminate();
    }
    
    public static void main(String[] args) {
        communication();
    }
}
