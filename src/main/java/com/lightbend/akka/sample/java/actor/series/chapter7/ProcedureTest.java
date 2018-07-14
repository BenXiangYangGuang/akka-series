package com.lightbend.akka.sample.java.actor.series.chapter7;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import com.lightbend.akka.sample.java.actor.series.chapter6.RouterTest;
import com.typesafe.config.ConfigFactory;

import java.lang.reflect.MalformedParametersException;

/**
 * @Author: fei2
 * @Date: 18-7-4 下午6:08
 * @Description:
 * @Refer To:
 */
public class ProcedureTest extends UntypedActor{

    private final LoggingAdapter log = Logging.getLogger(getContext().system(),this);

    Procedure<Object> happy = new Procedure<Object>() {
        @Override
        public void apply(Object o) throws Exception {
            log.info("i am happy! " + o);
            if (o == Msg.PLAY){
               getSender().tell("i am ready happy !!" ,getSelf());
               log.info("i am ready happy !!");
            } else if(o == Msg.SLEEP){
                log.info("i do not like sleep!");
                getContext().become(angry);
            } else {
                unhandled(o);
            }
        }
    };


    Procedure<Object> angry = new Procedure<Object>() {
        @Override
        public void apply(Object o) throws Exception {
            log.info("i am angray !" + o);
            if (o == Msg.SLEEP){
                getSender().tell("i am already angry !!" ,getSelf());
                log.info("i am already angry !!");
            } else if (o == Msg.PLAY){
                log.info("i like play.");
                getContext().become(happy);
            } else {
                unhandled(o);
            }
        }
    };

    @Override
    public void onReceive(Object message) throws Throwable {
        log.info("onReceive msg:" + message);

        if (message == Msg.SLEEP){
            getContext().become(angry);
        } else if (message == Msg.PLAY){
            getContext().become(happy);
        } else {
            unhandled(message);
        }
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("strategy", ConfigFactory.load("akka.config"));
        //actorRef 名称不能带有空格 example Procedure Test
        ActorRef procedureTest = system.actorOf(Props.create(ProcedureTest.class),"ProcedureTest");
        /**
         * ProcedureTest.onReceive(Object message),只有在第一次tell命令之时有用;
         * 有了procedure状态之后,再次接受发送的消息,就有这个状态的Procedure,来接受命令,从而执行接受到消息相应的操作;onReceive(Object message)不在有作用
         */
        procedureTest.tell(Msg.PLAY,ActorRef.noSender());
        procedureTest.tell(Msg.SLEEP,ActorRef.noSender());
        procedureTest.tell(Msg.PLAY,ActorRef.noSender());
        procedureTest.tell(Msg.PLAY,ActorRef.noSender());
        //
        procedureTest.tell(PoisonPill.getInstance(),ActorRef.noSender());
    }

    public enum Msg {
        SLEEP,
        PLAY
    }
}
