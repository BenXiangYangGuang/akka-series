package com.lightbend.akka.sample.java.actor.creator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Created by fei2 on 2018/4/27.
 *
 * 创建actor并进行通信
 */
public class SampleCreateTest {
    
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
        
        // 这里直接传入所需要创建的Actor对应的类
        Props props1 = Props.create(Actor1.class);
        Props props2 = Props.create(Actor2.class);
        
        // 使用Props创建Actor
        final ActorSystem system = ActorSystem.create("MySystem");
        final ActorRef actor1 = system.actorOf(props1, "Actor1");
        final ActorRef actor2 = system.actorOf(props2, "Actor2");
        actor2.tell(MSG.HI, actor1);
        
        system.stop(actor1);
        system.stop(actor2);
        system.terminate();
    }
    
    public static void main(String[] args) {
        communication();
    }
}
