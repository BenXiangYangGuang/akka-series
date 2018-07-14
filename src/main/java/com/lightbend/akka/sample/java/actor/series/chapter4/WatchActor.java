package com.lightbend.akka.sample.java.actor.series.chapter4;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by fei2 on 2018/5/12.
 * 监控actor
 */
public class WatchActor extends UntypedActor{
    
    private LoggingAdapter logging = Logging.getLogger(getContext().system(),this);
    /**
     * 监听一个actor
     * @param actorRef
     */
    public WatchActor(ActorRef actorRef) {
        getContext().watch(actorRef);
    }
    
    @Override
    public void onReceive(Object msg) throws Throwable {
        if(msg instanceof Terminated){
            logging.error(((Terminated) msg).getActor().path()+"has Terminated. now shutdown the system");
            getContext().system().terminate();
        }else {
            unhandled(msg);
        }
    }
}
