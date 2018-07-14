package com.lightbend.akka.sample.java.actor.series.chapter6;

import akka.actor.*;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lightbend.akka.sample.java.actor.series.chapter5.InboxTest;
import com.typesafe.config.ConfigFactory;

/**
 * Created by fei2 on 2018/5/12.
 */
public class RouterTest extends UntypedActor {
    
    public Router router;
    
    public static AtomicBoolean flag = new AtomicBoolean(true);
    
    
    {
        ArrayList<Routee> routees = new ArrayList<>();
        for (int i = 0; i < 5; i++){
            //借用上面的inboxActor
            ActorRef worker = getContext().actorOf(Props.create(InboxTest.class),"work" + i );
            getContext().watch(worker);
            routees.add(new ActorRefRoutee(worker));
        }
        /**
         * RoundRobinRoutingLogic: 轮询
         * BroadcastRoutingLogic: 广播
         * RandomRoutingLogic: 随机
         * SmallestMailboxRoutingLogic: 空闲
         */
        
        router = new Router(new RoundRobinRoutingLogic(),routees);
    }
    
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof InboxTest.Msg){
            router.route(message,getSender());
        }else if (message instanceof Terminated){
            router = router.removeRoutee(((Terminated) message).actor());
            System.out.println(((Terminated) message).actor().path()+ "该actor已经删除。router.size = "+router.routees().size());
            
            if (router.routees().size() == 0){
                System.out.println("没有可用的actor了，关闭系统。");
                flag.compareAndSet(true, false);
                getContext().system().terminate();
            }else {
                unhandled(message);
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("strategy", ConfigFactory.load("akka.config"));
        ActorRef routerTest = system.actorOf(Props.create(RouterTest.class),"RouterTest");
        
        int i = 1;
        while (flag.get()){
            routerTest.tell(InboxTest.Msg.WORKING,ActorRef.noSender());
            if(i % 10 ==0 ){
                routerTest.tell(InboxTest.Msg.CLOSE,ActorRef.noSender());
            }
            Thread.sleep(500);
            
            i++;
        }
    }
}
