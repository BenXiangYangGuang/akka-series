package com.lightbend.akka.sample.java.actor.start;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by fei2 on 2018/4/24.
 */
public class Printer extends AbstractActor{
    
    static public Props props(){
        return Props.create(Printer.class,() -> new Printer());
    }
    
    static public class Greeting{
        
        public final String message;
    
        public Greeting(String message) {
            this.message = message;
        }
        
    }
    
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(),this);
    
    public Printer(){
    
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Greeting.class, geeting -> {
            log.info(geeting.message);
        }).build();
    }
}
