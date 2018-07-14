import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fei2 on 2018/5/10.
 */
public class Test {
    
    private static Map<String,ActorRef> map = new HashMap<>();
    
    static class GreetOne extends UntypedActor{
        
        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof String){
                System.out.println("------------接受到消息one----------------");
            }
        }
    }
}
