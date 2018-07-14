package com.wewe.quickstart;

import akka.stream.*;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @Author: fei2
 * @Date: 18-7-10 下午3:19
 * @Description: akka stream quickstart 部分代码
 * @Refer To:
 */
public class QuickExample {
    public static void main(String[] args) {

        final ActorSystem system = ActorSystem.create("QuickStart");
        //使具体化,使物质化,使具体
        //materializer 他是一个数据流的执行工厂;使数据流的run()可以执行;你只需知道调用run()来使他执行
        final Materializer materializer = ActorMaterializer.create(system);
        //第一个参数为数据源类型,第二个参数没有使用就可以好用NotUse;
        // (e.g. a network source may provide information about the bound port or the peer’s address)
        final Source<Integer, NotUsed> source = Source.range(1, 100);
        source.runForeach(i -> System.out.println(i),materializer);
        //两个数据源的处理是异步的;分别进行输出.
        //提供一个数据源处理结束标志;done
        /*final CompletionStage<Done> done =
                source.runForeach(i -> System.out.println(i),materializer);
        done.thenRun(() -> system.terminate());
*/
        //1-100 每一个数的阶乘
        final Source<BigInteger,NotUsed> factorials = source
                .scan(BigInteger.ONE,(acc,next) -> acc.multiply(BigInteger.valueOf(next)));

        final CompletionStage<IOResult> result =
                factorials
                        .map(num -> ByteString.fromString(num.toString() + "\n"))
                .runWith(FileIO.toPath(Paths.get("factorials.txt")),materializer);
        List<String> list = new ArrayList<>();
        list.add("1");
        Object object = list;


    }

}
