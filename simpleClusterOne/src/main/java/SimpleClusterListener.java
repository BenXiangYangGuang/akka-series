import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

/**
 * @Author: fei2
 * @Date: 18-7-5 下午2:22
 * @Description:https://blog.csdn.net/liubenlong007/article/details/54601998
 * @Refer To: simpleClusterOne 项目和 simpleClusterTwo 构成了一个集群
 */
public class SimpleClusterListener extends UntypedActor{

    LoggingAdapter log = Logging.getLogger(getContext().system(),this);
    Cluster cluster = Cluster.get(getContext().system());

    @Override
    public void preStart() throws Exception {
        //#subscribe
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof MemberUp){
            MemberUp mUp = (MemberUp) message;
            log.info("Member is Up:{}",mUp.member());
        } else if (message instanceof UnreachableMember){
            UnreachableMember mUnreachable = (UnreachableMember) message;
            log.info("Member detected as unreachable: {}",mUnreachable.member());
        } else if (message instanceof MemberRemoved){
            MemberRemoved mRemoved = (MemberRemoved) message;
            log.info("Member is Removed : {}",mRemoved.member());
        } else if (message instanceof MemberEvent){

        } else {
            unhandled(message);
        }

    }

    public static void main(String[] args) {
        System.out.println("Start simpleClusterListener");
        ActorSystem system = ActorSystem.create("akkaClusterTest", ConfigFactory.load("reference.conf"));
        system.actorOf(Props.create(SimpleClusterListener.class),"simpleClusterListener");
        System.out.println("Started simpleClusterListener");
        ActorRef greetActor = system.actorOf(Props.create(Test.GreetOne.class),"greetOne");
        greetActor.tell(new String(),ActorRef.noSender());

    }
}
