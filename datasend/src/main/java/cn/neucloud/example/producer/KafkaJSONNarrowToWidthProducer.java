package cn.neucloud.example.producer;

import cn.neucloud.example.utils.KafkaProducerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author: fei2
 * @Date:2018/6/12 17:23
 * @Description:
 * @Refer To:
 */
@Slf4j
@Component
public class KafkaJSONNarrowToWidthProducer implements CommandLineRunner {

    //    private static String topic = "topic_zYy5r8J0";
    private static String topic = "json_narrow_to_width";
    private static long time = 3000;
    int v = 1 ;
    public void sendCsvData(){
        long date = System.currentTimeMillis();

        for (int i = 1; i < 5; i++){

            String value = "{\"t1\": "+date+",\"key\":\"col"+i+"\",\"value\": "+getRandomValue()+"}";

            KafkaProducerUtil.sendToKafka(topic,value);
            log.info("send {} to topic :{}" ,value,topic);
        }

    }

    private int getRandomValue(){
        return (int)(Math.random() * 1000);
    }

    @Override
    public void run(String... strings) throws Exception {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendCsvData();
            }
        }, 0, time);
    }
}
