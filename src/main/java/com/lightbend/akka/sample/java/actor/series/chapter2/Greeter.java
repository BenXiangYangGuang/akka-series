package com.lightbend.akka.sample.java.actor.series.chapter2;

import akka.actor.UntypedActor;

/**
 * Created by fei2 on 2018/5/11.
 * self() 和getSelf() 是actorRef 自己
 * getSender 是发送者
 * actorRef.noSender(),发送消息之后，kill自己,能接受消息，不能进行tell 消息
 * 参考：https://blog.csdn.net/liubenlong007/article/details/53782971
 */
public class Greeter extends UntypedActor {
    
    @Override
    public void onReceive(Object message) throws Throwable {
        if(message == Msg.GREET){
                System.out.println(getSender().isTerminated());
                System.out.println("greeter接受消息是否存活"+getSender().path());
                getSender().tell(Msg.DONE,self());
        }else{
            unhandled(message);
        }
    }
    
    public static enum Msg {
        GREET,DONE
    }
    
    
    
}
