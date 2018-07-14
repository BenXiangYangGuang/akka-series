package com.lightbend.akka.sample.java.actor.creator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fei2 on 2018/4/27.
 *
 * 通过creator 创建actor
 */
public class CreatorCreateTest {
    
    enum MSG {
        HI, OK
    }
    
    static class Creator1 implements Creator<Actor1> {
        // 多线程保证
        AtomicInteger index = new AtomicInteger();
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        
        public Actor1 create() throws Exception {
            // TODO Auto-generated method stub
            return new Actor1("Actor1", index.getAndAdd(1));
        }
        
    }
    static class Creator2 implements Creator<Actor2>{
        // 多线程保证
        AtomicInteger index = new AtomicInteger();
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        
        public Actor2 create() throws Exception {
            // TODO Auto-generated method stub
            return new Actor2("Actor2", index.getAndAdd(1));
        }
        
    }

    
    static class Actor1 extends UntypedActor {
        String name;
        int index;
        
        Actor1(String name, int index){
            this.name = name;
            this.index = index;
        }
        
        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println(name + "_" + index + " receive message");
            if (message instanceof MSG) {
                if (message.equals(MSG.OK)) {
                    System.out.println("i receive ok");
                } else {
                    unhandled(message);
                }
            } else {
                unhandled(message);
            }
            
        }
        
    }
    
    static class Actor2 extends UntypedActor {
        String name;
        int index;
        
        Actor2(String name, int index){
            this.name = name;
            this.index = index;
        }
        
        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println(name + "_" + index + " receive message");
            if (message instanceof MSG) {
                if (message.equals(MSG.HI)) {
                    System.out.println("i receive hi");
                    getSender().tell(MSG.OK, getSelf());
                } else {
                    unhandled(message);
                }
            } else {
                unhandled(message);
            }
        }
        
    }
    
    public static void communication(){
        
        // 创建Creator实例，这个Creator用于创建Actor，然后将Creator传入到Props中
        Creator1 creator1 = new Creator1();
        Creator2 creator2 = new Creator2();
        Props props1 = Props.create(creator1);
        Props props2 = Props.create(creator2);
        
        // 使用Props创建Actor
        final ActorSystem system = ActorSystem.create("MySystem");
        final ActorRef actor1 = system.actorOf(props1);
        final ActorRef actor2 = system.actorOf(props2);
        actor2.tell(MSG.HI, actor1);
    //前面的例子中创建Actor都是调用的SystemActor，使用这种方法创建的Actor属于顶级Actor，它们由系统提供的guardian actor直接监管
        System.out.println(system.guardian().path());
        System.out.println(actor1.path());
        System.out.println(actor2.path());
    
        system.stop(actor1);
        system.stop(actor2);
        system.terminate();
    }
    
    public static void main(String[] args) {
        communication();
    }
}
