package com.lightbend.akka.sample.java.actor.series.chapter4;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by fei2 on 2018/5/12.
 */
public class MyWork extends UntypedActor {
    
    private LoggingAdapter logging = Logging.getLogger(getContext().system(),this);
    
    public static enum Msg {
        WORKING, DONE, CLOSE
    }
    
    @Override
    public void preStart() throws Exception {
        logging.info("myWork starting");
    }
    
    @Override
    public void postStop() throws Exception {
        logging.info("MyWork stoping");
    }
    
    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg == Msg.WORKING){
            logging.info("i am working");
        }else if (msg == Msg.DONE){
            logging.info("stop working");
        } else if(msg == Msg.CLOSE){
            logging.info("stop close");
            getSender().tell(Msg.CLOSE,getSelf());
            getContext().stop(getSelf());
        }else {
            unhandled(msg);
        }
    }
}
