package com.lightbend.akka.sample.java.actor.series.chapter4;

import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.japi.Function;
import scala.concurrent.duration.Duration;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by fei2 on 2018/5/12.
 * 监控者
 */
public class SuperVisor extends UntypedActor {
    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof Props){
            getContext().actorOf((Props) msg,"restartActor");
        }
    }
    
    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(3, Duration.create(1, TimeUnit.MINUTES), new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
            public SupervisorStrategy.Directive apply(Throwable throwable) throws Exception {
                if( throwable instanceof ArithmeticException){
                    System.out.println("meet ArithmeticException ,just resume");
                    return SupervisorStrategy.resume(); //继续；重新开始；恢复职位；
                }else if (throwable instanceof NullPointerException){
                    System.out.println("meet NullPointerException ,restart");
                    return SupervisorStrategy.restart();
                }else if (throwable instanceof IllegalArgumentException){
                    System.out.println("meet IllegalArgumentException ,stop");
                    return SupervisorStrategy.stop();
                }else {
                    System.out.println("escalate");
                    return SupervisorStrategy.escalate();  //使逐步升级；使逐步上升；乘自动梯上升；
                }
            }
        });
    }
}
